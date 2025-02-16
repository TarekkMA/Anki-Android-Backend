/*
 * Copyright (c) 2021 David Allison <davidallisongithub@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.ankiweb.rsdroid.database;

import net.ankiweb.rsdroid.BackendException;
import net.ankiweb.rsdroid.RustV1Cleanup;

import org.json.JSONArray;
import org.json.JSONException;

import timber.log.Timber;

/**
 * A cursor to the Rust implementation of the SQLite library which performs paging via a LIMIT/OFFSET
 * mechanism.
 *
 * This is used to avoid memory pressure - the Rust code does not perform any streaming of records,
 * we don't yet handle OOMs in Java, and we have a limited heap to work with.
 *
 * We assume that the provided query does not have a limit/offset and is a select query.
 */
@RustV1Cleanup("Not currently used. Convert to use protobuf instead of JSON")
public class LimitOffsetSQLiteCursor extends AnkiJsonDatabaseCursor {

    /**
     * The number of records which should be obtained in a batch using {@link LimitOffsetSQLiteCursor}
     */
    public static int PAGE_SIZE = 100;

    private int position = -1;
    private int pageOffset = -1;
    private final int pageSize = PAGE_SIZE;

    public LimitOffsetSQLiteCursor(Session backend, String query, Object[] bindArgs) {
        super(backend, query, bindArgs);
        loadPage();
    }


    private void loadPage() {
        pageOffset++;
        position = -1;

        String query = this.query + " LIMIT " + getPageSize() + " OFFSET " + pageOffset * getPageSize();
        try {
            results = super.fullQuery(query, bindArgs);
        } catch (BackendException e) {
            throw e.toSQLiteException(query);
        }
    }

    @Override
    public int getCount() {
        Timber.w("Extremely slow call: getCount() on '%s'", query);

        // Consider wrapping the query with a "select count() from (`query`)"
        // will that work in all cases?
        int currentPageOffset = pageOffset;
        int currentPosition = position;

        int knownCount = pageOffset * (getPageSize() - 1) + results.length();

        if (results.length() > 0 && results.length() < getPageSize()) {
            return knownCount;
        }

        // HACK: we could work from the knownCount, but it feels better to write simple code which works.
        reset();

        int count = 0;

        while (results.length() == PAGE_SIZE) {
            loadPage();
            count += results.length();
        }

        // reload the current page
        pageOffset = currentPageOffset -1;
        loadPage();

        // reset the variables
        pageOffset = currentPageOffset;
        position = currentPosition;

        return count;
    }

    private void reset() {
        results = null;
        pageOffset = -1;
        position = -1;
    }

    @Override
    public int getPosition() {
        return pageOffset * getPageSize() + position;
    }

    @Override
    public boolean moveToFirst() {
        if (results.length() == 0) {
            return false;
        }
        position = 0;
        return true;
    }

    @Override
    public boolean moveToNext() {
        // This will overrun by 1 row if there are PageSize elements. That shouldn't cause a problem here.
        if (results.length() > 0 && position + 1 >= getPageSize()) {
            loadPage();
        }
        position++;
        return results.length() != 0 && position < results.length();
    }

    @Override
    public void close() {

    }

    @Override
    protected JSONArray getRowAtCurrentPosition() throws JSONException {
        return results.getJSONArray(position);
    }

    public int getPageSize() {
        return pageSize;
    }
}

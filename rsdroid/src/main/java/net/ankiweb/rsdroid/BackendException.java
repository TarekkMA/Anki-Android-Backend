/*
 * Copyright (c) 2020 David Allison <davidallisongithub@gmail.com>
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

package net.ankiweb.rsdroid;

import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import BackendProto.Backend;

public class BackendException extends RuntimeException {
    @SuppressWarnings({"unused", "RedundantSuppression"})
    @Nullable
    private final Backend.BackendError error;

    public BackendException(Backend.BackendError error)  {
        super(error.getLocalized());
        this.error = error;
    }

    public BackendException(String message) {
        super(message);
        error = null;
    }

    public static BackendException fromError(Backend.BackendError error) {
        if (error.hasDbError()) {
            return new BackendDbException(error);
        }
        // This should have produced a hasFatalError property?
        if (error.getValueCase() == Backend.BackendError.ValueCase.FATAL_ERROR) {
            throw new BackendFatalError(error.getFatalError());
        }

        return new BackendException(error);
    }

    public static RuntimeException fromException(Exception ex) {
        return new RuntimeException(ex);
    }


    public RuntimeException toSQLiteException(String query) {
        String message = String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, this.getLocalizedMessage());
        return new SQLiteException(message, this);
    }

    public static class BackendDbException extends BackendException {

        public BackendDbException(Backend.BackendError error) {
            // This is very simple for now and matches Anki Desktop (error is currently text)
            // Later on, we may want to use structured error messages
            // DBError { info: "SqliteFailure(Error { code: Unknown, extended_code: 1 }, Some(\"no such table: aa\"))", kind: Other }
            super(error);
        }

        @Override
        public RuntimeException toSQLiteException(String query) {
            String message = this.getLocalizedMessage();

            if (message == null) {
                String outMessage = String.format(Locale.ROOT, "Unknown error while compiling: \"%s\"", query);
                throw new SQLiteException(outMessage, this);
            }

            if (message.contains("InvalidParameterCount")) {
                Matcher p = Pattern.compile("InvalidParameterCount\\((\\d*), (\\d*)\\)").matcher(this.getMessage());
                if (p.find()) {
                    int paramCount = Integer.parseInt(p.group(1));
                    int index = Integer.parseInt(p.group(2));
                    String errorMessage = String.format(Locale.ROOT, "Cannot bind argument at index %d because the index is out of range.  The statement has %d parameters.", index, paramCount);
                    throw new IllegalArgumentException(errorMessage, this);
                }
            } else if (message.contains("ConstraintViolation")) {
                throw new SQLiteConstraintException(message);
            } else if (message.contains("DiskFull")) {
                throw new SQLiteFullException(message);
            } else if (message.contains("DatabaseCorrupt")) {
                String outMessage = String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, message);
                throw new SQLiteDatabaseCorruptException(outMessage);
            }

            String outMessage = String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, message);
            throw new SQLiteException(outMessage, this);
        }
    }
}

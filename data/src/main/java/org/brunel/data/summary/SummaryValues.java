/*
 * Copyright (c) 2015 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.brunel.data.summary;

import org.brunel.data.Data;
import org.brunel.data.Field;
import org.brunel.data.util.DateFormat;
import org.brunel.data.util.ItemsList;
import org.brunel.data.util.Range;

import java.util.ArrayList;
import java.util.List;

public final class SummaryValues {
    private final Field[] fields;                                    // the fields we use
    public final List<Integer> rows = new ArrayList<Integer>();    // Which data rows have been aggregated into this
    public double[] percentSums;

    public SummaryValues(Field[] fields) {
        this.fields = fields;
    }

    public int firstRow() {
        return rows.get(0);
    }

    /*
     * Possible summaries are:
     * [numeric] mean, min, max, range, iqr, median, stddev
     * [any] count, mode, unique
     */
    public Object get(int fieldIndex, String summary, String option, DateFormat df) {
        if (summary.equals("count")) return rows.size();

        Object[] data = new Object[rows.size()];
        for (int i = 0; i < data.length; i++)
            data[i] = fields[fieldIndex].value(rows.get(i));

        Field f = Data.makeColumnField("temp", null, data);

        if (summary.equals("percent")) {
            double sum = percentSums[fieldIndex];
            return sum > 0 ? 100 * f.getNumericProperty("mean") * f.getNumericProperty("valid") / sum : null;
        }

        if (summary.equals("range")) {
            Double low = f.getNumericProperty("min");
            Double high = f.getNumericProperty("max");
            return low == null ? null : Range.make(low, high, df);
        }
        if (summary.equals("iqr")) {
            Double low = f.getNumericProperty("q1");
            Double high = f.getNumericProperty("q3");
            return low == null ? null : Range.make(low, high, df);
        }

        if (summary.equals("sum"))
            return f.getNumericProperty("mean") * f.getNumericProperty("valid");
        if (summary.equals("list")) {
            ItemsList categories = new ItemsList((Object[]) f.getProperty("categories"), df);
            if (option != null) {
                int displayCount = Integer.parseInt(option);
                categories.setDisplayCount(displayCount);
            }
            return categories;
        }
        return f.getProperty(summary);
    }

}

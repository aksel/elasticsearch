/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.client.ml.dataframe.evaluation.regression;

import org.elasticsearch.client.ml.dataframe.evaluation.EvaluationMetric;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Objects;

import static org.elasticsearch.common.xcontent.ConstructingObjectParser.constructorArg;
import static org.elasticsearch.common.xcontent.ConstructingObjectParser.optionalConstructorArg;

/**
 * Calculates the pseudo Huber loss function.
 *
 * equation: pseudohuber = 1/n * Σ(δ^2 * sqrt(1 + a^2 / δ^2) - 1)
 * where: a = y - y´
 *        δ - parameter that controls the steepness
 */
public class PseudoHuberMetric implements EvaluationMetric {

    public static final String NAME = "pseudo_huber";

    public static final ParseField DELTA = new ParseField("delta");

    private static final ConstructingObjectParser<PseudoHuberMetric, Void> PARSER =
        new ConstructingObjectParser<>(NAME, true, args -> new PseudoHuberMetric((Double) args[0]));

    static {
        PARSER.declareDouble(optionalConstructorArg(), DELTA);
    }

    public static PseudoHuberMetric fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }

    private final Double delta;

    public PseudoHuberMetric(@Nullable Double delta) {
        this.delta = delta;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (delta != null) {
            builder.field(DELTA.getPreferredName(), delta);
        }
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PseudoHuberMetric that = (PseudoHuberMetric) o;
        return Objects.equals(this.delta, that.delta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delta);
    }

    public static class Result implements EvaluationMetric.Result  {

        public static final ParseField VALUE = new ParseField("value");
        private final double value;

        public static Result fromXContent(XContentParser parser) {
            return PARSER.apply(parser, null);
        }

        private static final ConstructingObjectParser<Result, Void> PARSER =
            new ConstructingObjectParser<>("pseudo_huber_result", true, args -> new Result((double) args[0]));

        static {
            PARSER.declareDouble(constructorArg(), VALUE);
        }

        public Result(double value) {
            this.value = value;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(VALUE.getPreferredName(), value);
            builder.endObject();
            return builder;
        }

        public double getValue() {
            return value;
        }

        @Override
        public String getMetricName() {
            return NAME;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Result that = (Result) o;
            return Objects.equals(that.value, this.value);
        }

        @Override
        public int hashCode() {
            return Double.hashCode(value);
        }
    }
}

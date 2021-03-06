/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.painless.node;

import org.elasticsearch.painless.AnalyzerCaster;
import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.ir.CastNode;
import org.elasticsearch.painless.ir.ClassNode;
import org.elasticsearch.painless.ir.ExpressionNode;
import org.elasticsearch.painless.lookup.PainlessCast;
import org.elasticsearch.painless.symbol.Decorations.Explicit;
import org.elasticsearch.painless.symbol.Decorations.Internal;
import org.elasticsearch.painless.symbol.Decorations.PartialCanonicalTypeName;
import org.elasticsearch.painless.symbol.Decorations.StaticType;
import org.elasticsearch.painless.symbol.Decorations.TargetType;
import org.elasticsearch.painless.symbol.Decorations.ValueType;
import org.elasticsearch.painless.symbol.SemanticScope;

/**
 * The superclass for all E* (expression) and P* (postfix) nodes.
 */
public abstract class AExpression extends ANode {

    public static class Output {

        /**
         * The {@link ExpressionNode}(s) generated from this expression.
         */
        ExpressionNode expressionNode = null;
    }

    /**
     * Standard constructor with location used for error tracking.
     */
    AExpression(int identifier, Location location) {
        super(identifier, location);
    }

    /**
     * Checks for errors and collects data for the writing phase.
     */
    Output analyze(ClassNode classNode, SemanticScope semanticScope) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks for errors and collects data for the writing phase. Adds additional, common
     * error checking for conditions related to static types and partially constructed static types.
     */
    static Output analyze(AExpression expression, ClassNode classNode, SemanticScope semanticScope) {
        Output output = expression.analyze(classNode, semanticScope);

        if (semanticScope.hasDecoration(expression, PartialCanonicalTypeName.class)) {
            throw expression.createError(new IllegalArgumentException("cannot resolve symbol " +
                    "[" + semanticScope.getDecoration(expression, PartialCanonicalTypeName.class).getPartialCanonicalTypeName() + "]"));
        }

        if (semanticScope.hasDecoration(expression, StaticType.class)) {
            throw expression.createError(new IllegalArgumentException("value required: instead found unexpected type " +
                    "[" + semanticScope.getDecoration(expression, StaticType.class).getStaticCanonicalTypeName() + "]"));
        }

        if (semanticScope.hasDecoration(expression, ValueType.class) == false) {
            throw expression.createError(new IllegalStateException("value required: instead found no value"));
        }

        return output;
    }

    // TODO: move this somewhere more appropriate
    public PainlessCast cast(SemanticScope semanticScope) {
        Class<?> valueType = semanticScope.getDecoration(this, ValueType.class).getValueType();
        Class<?> targetType = semanticScope.getDecoration(this, TargetType.class).getTargetType();
        boolean isExplicitCast = semanticScope.getCondition(this, Explicit.class);
        boolean isInternalCast = semanticScope.getCondition(this, Internal.class);

        return AnalyzerCaster.getLegalCast(getLocation(), valueType, targetType, isExplicitCast, isInternalCast);
    }

    static ExpressionNode cast(ExpressionNode expressionNode, PainlessCast painlessCast) {
        if (painlessCast == null) {
            return expressionNode;
        }

        CastNode castNode = new CastNode();
        castNode.setLocation(expressionNode.getLocation());
        castNode.setExpressionType(painlessCast.targetType);
        castNode.setCast(painlessCast);
        castNode.setChildNode(expressionNode);

        return castNode;
    }
}

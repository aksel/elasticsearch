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

import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.ir.BlockNode;
import org.elasticsearch.painless.ir.ClassNode;
import org.elasticsearch.painless.symbol.Decorations.AllEscape;
import org.elasticsearch.painless.symbol.Decorations.AnyBreak;
import org.elasticsearch.painless.symbol.Decorations.AnyContinue;
import org.elasticsearch.painless.symbol.Decorations.BeginLoop;
import org.elasticsearch.painless.symbol.Decorations.InLoop;
import org.elasticsearch.painless.symbol.Decorations.LastLoop;
import org.elasticsearch.painless.symbol.Decorations.LastSource;
import org.elasticsearch.painless.symbol.Decorations.LoopEscape;
import org.elasticsearch.painless.symbol.Decorations.MethodEscape;
import org.elasticsearch.painless.symbol.SemanticScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a set of statements as a branch of control-flow.
 */
public class SBlock extends AStatement {

    private final List<AStatement> statementNodes;

    public SBlock(int identifier, Location location, List<AStatement> statementNodes) {
        super(identifier, location);

        this.statementNodes = Collections.unmodifiableList(Objects.requireNonNull(statementNodes));
    }

    public List<AStatement> getStatementNodes() {
        return statementNodes;
    }

    @Override
    Output analyze(ClassNode classNode, SemanticScope semanticScope) {
        Output output = new Output();

        if (statementNodes.isEmpty()) {
            throw createError(new IllegalArgumentException("A block must contain at least one statement."));
        }

        AStatement last = statementNodes.get(statementNodes.size() - 1);

        List<Output> statementOutputs = new ArrayList<>(statementNodes.size());

        boolean lastSource = semanticScope.getCondition(this, LastSource.class);
        boolean beginLoop = semanticScope.getCondition(this, BeginLoop.class);
        boolean inLoop = semanticScope.getCondition(this, InLoop.class);
        boolean lastLoop = semanticScope.getCondition(this, LastLoop.class);

        boolean allEscape = false;
        boolean anyContinue = false;
        boolean anyBreak = false;

        for (AStatement statement : statementNodes) {
            if (inLoop) {
                semanticScope.setCondition(statement, InLoop.class);
            }

            if (statement == last) {
                if (beginLoop || lastLoop) {
                    semanticScope.setCondition(statement, LastLoop.class);
                }

                if (lastSource) {
                    semanticScope.setCondition(statement, LastSource.class);
                }
            }

            Output statementOutput = statement.analyze(classNode, semanticScope);
            allEscape = semanticScope.getCondition(statement, AllEscape.class);

            if (statement == last) {
                semanticScope.replicateCondition(statement, this, MethodEscape.class);
                semanticScope.replicateCondition(statement, this, LoopEscape.class);

                if (allEscape) {
                    semanticScope.setCondition(this, AllEscape.class);
                }
            } else {
                // Note that we do not need to check after the last statement because
                // there is no statement that can be unreachable after the last.
                if (allEscape) {
                    throw createError(new IllegalArgumentException("Unreachable statement."));
                }
            }

            anyContinue |= semanticScope.getCondition(statement, AnyContinue.class);
            anyBreak |= semanticScope.getCondition(statement, AnyBreak.class);;

            statementOutputs.add(statementOutput);
        }

        if (anyContinue) {
            semanticScope.setCondition(this, AnyContinue.class);
        }

        if (anyBreak) {
            semanticScope.setCondition(this, AnyBreak.class);
        }

        BlockNode blockNode = new BlockNode();

        for (Output statementOutput : statementOutputs) {
            blockNode.addStatementNode(statementOutput.statementNode);
        }

        blockNode.setLocation(getLocation());
        blockNode.setAllEscape(allEscape);

        output.statementNode = blockNode;

        return output;
    }
}

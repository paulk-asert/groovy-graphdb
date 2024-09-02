/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.groovy.typecheckers.CheckingVisitor
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression

beforeMethodCall { call ->
    if (call.methodAsString == 'coaches') {
        var lhsType = call.objectExpression.accessedVariable.getNodeMetaData('VERTEX_SUBTYPE')
        var rhsType = call.arguments[0].accessedVariable.getNodeMetaData('VERTEX_SUBTYPE')
        if (lhsType == 'Coach' && rhsType == 'Swimmer') {
            handled = true
        } else {
            addStaticTypeError("Invalid edge - expected: <Coach>.coaches(<Swimmer>)\nbut found: <$lhsType>.coaches(<$rhsType>)", call)
        }
    }
}

beforeVisitMethod { method ->
    method.code.visit(new CheckingVisitor() {
        @Override
        void visitDeclarationExpression(DeclarationExpression expression) {
            var left = expression.leftExpression
            if (left instanceof VariableExpression) {
                var right = expression.rightExpression
                if (right instanceof MethodCallExpression) {
                    right = right.objectExpression
                    if (right instanceof MethodCallExpression) {
                        var oe = right.objectExpression
                        if (oe instanceof MethodCallExpression && oe.methodAsString == 'addV') {
                            var args = oe.arguments
                            if (args instanceof ArgumentListExpression) {
                                left.accessedVariable.putNodeMetaData('VERTEX_SUBTYPE', args.getExpression(0).value)
                            }
                        }
                    }
                }
            }
            super.visitDeclarationExpression(expression)
        }
    })
}


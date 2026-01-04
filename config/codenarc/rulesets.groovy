/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
ruleset {
    ruleset('rulesets/basic.xml') {
        ThrowExceptionFromFinallyBlock {
            enabled = false
        }
        EmptyCatchBlock {
            enabled = false
        }
    }
    ruleset('rulesets/braces.xml')
    ruleset('rulesets/convention.xml') {
        InvertedIfElse {
            enabled = false
        }
        NoDef {
            enabled = false
        }
        NoDouble {
            enabled = false
        }
        TrailingComma {
            enabled = false
        }
        MethodReturnTypeRequired {
            enabled = false
        }
        VariableTypeRequired {
            enabled = false
        }
        FieldTypeRequired {
            enabled = false
        }
        MethodParameterTypeRequired {
            enabled = false
        }
        CompileStatic {
            enabled = false
        }
        ImplicitClosureParameter {
            enabled = false
        }
        ImplicitReturnStatement {
            enabled = false
        }
    }
    ruleset('rulesets/comments.xml') {
        ClassJavadoc {
            enabled = false
        }
        SpaceAfterCommentDelimiter {
            enabled = false
        }
    }
    ruleset('rulesets/dry.xml') {
        DuplicateListLiteral {
            enabled = false
        }
        DuplicateStringLiteral {
            enabled = false
        }
        DuplicateNumberLiteral {
            enabled = false
        }
    }
    ruleset('rulesets/formatting.xml') {
        BracesForForLoop {
            doNotApplyToFileNames = 'NeoAthletes.groovy, NeoSwimmers.groovy'
        }
        ClassEndsWithBlankLine {
            enabled = false
        }
        ClassStartsWithBlankLine {
            enabled = false
        }
        FileEndsWithoutNewline {
            enabled = false
        }
        Indentation {
            enabled = false
        }
        LineLength {
            length = 200
        }
        SpaceAfterComma {
            enabled = false // fooled by emojis
        }
        SpaceAfterOpeningBrace {
            ignoreEmptyBlock = true
        }
        SpaceBeforeClosingBrace {
            ignoreEmptyBlock = true
        }
        SpaceAroundMapEntryColon {
            characterAfterColonRegex = /\s/
            characterBeforeColonRegex = /.*/
        }
    }
    ruleset('rulesets/generic.xml') {
        RequiredString {
            string = 'Apache License, Version 2.0'
            violationMessage = 'Copyright header not found'
        }
    }
    ruleset('rulesets/groovyism.xml') {
        ExplicitHashSetInstantiation {
            enabled = false
        }
        GetterMethodCouldBeProperty {
            enabled = false
        }
        ExplicitCallToDivMethod {
            enabled = false
        }
        ExplicitCallToModMethod {
            enabled = false
        }
        ExplicitLinkedListInstantiation {
            enabled = false
        }
    }
    ruleset('rulesets/imports.xml') {
        MisorderedStaticImports {
            comesBefore = false
        }
        NoWildcardImports {
            enabled = false
        }
        UnnecessaryGroovyImport {
            doNotApplyToFileNames = 'ExceptionToPngConverter.groovy'
        }
    }
    ruleset('rulesets/naming.xml') {
        ClassNameSameAsSuperclass {
            enabled = false
        }
        ConfusingMethodName {
            enabled = false
        }
        FactoryMethodName {
            enabled = false
        }
        FieldName {
            ignoreFieldNames = 'ran,swam,supersedes,runnerup'
        }
        MethodName {
            regex = /([a-z]\w*|\$)/
        }
        VariableName {
            ignoreVariableNames = 'DB_URL,PASS,USER'
        }
    }
    ruleset('rulesets/unnecessary.xml') {
        UnnecessaryGetter {
            enabled = false
        }
        UnnecessaryGString {
            enabled = false
        }
        UnnecessaryObjectReferences {
            enabled = false
        }
        UnnecessaryPackageReference {
            enabled = false
        }
        UnnecessaryReturnKeyword {
            enabled = false
        }
    }
    ruleset('rulesets/unused.xml') {
        UnusedVariable {
            ignoreVariableNames = 'swim*,athlete*,marathon*'
            doNotApplyToFileNames = 'NeoAthletes.groovy, NeoSwimmers.groovy' // doesn't understand G5 syntax
        }
    }
}

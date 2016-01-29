/*
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.text.names.modifiedREB;

import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;


public final class HumanNameParser {

    /**
     * The options used by the parser.
     */
    private final ParserOptions options;

    /*
     * Regular expressions used by the parser.
     */

    // The regex use is a bit tricky.  *Everything* matched by the regex will be replaced,
    // but you can select a particular parenthesized submatch to be returned.
    // Also, note that each regex requres that the preceding ones have been run, and matches chopped out.
    // names that starts or end w/ an apostrophe break this
    private final static Pattern NICKNAMES_REGEX = Pattern.compile("(?i) ('|\\\"|\\(\\\"*'*)(.+?)('|\\\"|\\\"*'*\\)) ");
    // note the lookahead, which isn't returned or replaced
    private final static Pattern LEADING_INIT_REGEX = Pattern.compile("(?i)(^(.\\.*)(?= \\p{L}{2}))");
    private final static Pattern FIRST_NAME_REGEX = Pattern.compile("(?i)^([^ ]+)");
    private Pattern suffixRegex;
    private Pattern lastRegex;
    
    /**
     * Creates a new parser.
     */
    public HumanNameParser() {
        this(ParserOptions.DEFAULT_OPTIONS);
    }

    /**
     * Creates a new parser by providing options.
     */
    public HumanNameParser(ParserOptions options) {
        this.options = options;
        final String suffixes = StringUtils.join(options.getSuffixes(), "\\.*|") + "\\.*";
        final String prefixes = StringUtils.join(options.getPrefixes(), " |") + " ";
        suffixRegex = Pattern.compile("(?i),* *((" + suffixes + ")$)");
        lastRegex = Pattern.compile("(?i)(?!^)\\b([^ ]+ y |" + prefixes + ")*[^ ]+$");
    }

    /**
     * Gets the parser options.
     *
     * @return parser options
     */
    public ParserOptions getOptions() {
        return options;
    }

    /**
     * Parses a name from the given string.
     *
     * @param name the name to parse. Must not be null.
     * @throws NameParseException if the parser fails to retrieve the name parts.
     * @throws NullPointerException if name is null.
     */
    public Name parse(String name) {
        Objects.requireNonNull(name, "Parameter 'name' must not be null.");

        NameString nameString = new NameString(name);

        // get nickname, if there is one
        String nickname = nameString.chopWithRegex(NICKNAMES_REGEX, 2);

        // get suffix, if there is one
        String suffix = nameString.chopWithRegex(suffixRegex, 1);

        // flip the before-comma and after-comma parts of the name
        nameString.flip(",");

        // get the last name
        String last = nameString.chopWithRegex(lastRegex, 0);

        // get the first initial, if there is one
        String leadingInit = nameString.chopWithRegex(LEADING_INIT_REGEX, 1);

        // get the first name
        String first = nameString.chopWithRegex(FIRST_NAME_REGEX, 0);
        if (StringUtils.isBlank(first)) {
            throw new NameParseException("Couldn't find a first name in '{" + nameString.getWrappedString() + "}'");
        }

        // if anything's left, that's the middle name
        String middle = nameString.getWrappedString();
        
        return new Name(leadingInit, first, nickname, middle, last, suffix);
    }

}

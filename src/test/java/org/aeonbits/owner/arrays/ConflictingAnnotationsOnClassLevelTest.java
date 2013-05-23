/*
 * Copyright (c) 2013, Luigi R. Viggiano
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package org.aeonbits.owner.arrays;

import org.aeonbits.owner.ArraySupportTest.CustomCommaTokenizer;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Separator;
import org.aeonbits.owner.Config.TokenizerClass;
import org.aeonbits.owner.ConfigFactory;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author luigi
 */
public class ConflictingAnnotationsOnClassLevelTest {

    private ConflictingAnnotationsOnClassLevel cfgConflictingAnnotationsOnClassLevel;

    @Before
    public void before() {
        cfgConflictingAnnotationsOnClassLevel = ConfigFactory.create(ConflictingAnnotationsOnClassLevel.class);
    }

    @TokenizerClass(CustomCommaTokenizer.class) // should throw an exception when the first array conversion is invoked:
    @Separator(",")                             // @Tokenizer and @Separator annotations cannot be used together on
    // class level.
    public static interface ConflictingAnnotationsOnClassLevel extends Config {
        @DefaultValue("1, 2, 3, 4")
        public int[] commaSeparated();
    }

    @Test
    public void testConflictingAnnotationsOnClassLevel() throws Throwable {
        try {
            cfgConflictingAnnotationsOnClassLevel.commaSeparated();
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ex) {
            assertThat(ex.getMessage(),
                    equalTo("You cannot specify @Separator and @TokenizerClass both together on class level for 'org" +
                            ".aeonbits.owner.arrays.ConflictingAnnotationsOnClassLevelTest" +
                            ".ConflictingAnnotationsOnClassLevel'"));
        }
    }

}
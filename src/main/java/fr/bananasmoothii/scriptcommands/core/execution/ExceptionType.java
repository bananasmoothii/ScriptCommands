/*
 * Copyright 2020 ScriptCommands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.bananasmoothii.scriptcommands.core.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ExceptionType {
    private static final List<ExceptionType> types = new ArrayList<>();

    public ExceptionType() {
        types.add(this);
    }

    public static List<ExceptionType> getAllExceptionTypes() {
        return Collections.unmodifiableList(types);
    }

    public abstract String getName();

    public abstract String getDescription();

    public abstract String[] getDocumentationLinks();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getDescription());
        String[] documentationLinks = getDocumentationLinks();
        if (documentationLinks.length != 0) {
            sb.append("\nSee the documentation:");
            for (String documentationLink : documentationLinks) {
                sb.append("\n-> ").append(documentationLink);
            }
        }
        return sb.toString();
    }


    // Here are some default exceptions

    // TODO: fill up documentation links

    public static final ExceptionType ASSERTION_ERROR = new ExceptionType() {
        @Override public String getName() {
            return "ASSERTION_ERROR";
        }
        @Override public String getDescription() {
            return "An assertion error happens when when using the \"assert\" keyword but the expression " +
                    "evaluates to false.";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[0];
        }
    };

    public static final ExceptionType CONVERSION_ERROR = new ExceptionType() {
        @Override public String getName() {
            return "CONVERSION_ERROR";
        }
        @Override public String getDescription() {
            return "A conversion error happens when you want to convert a type to another. For example, " +
                    "you can convert \"12.8\" (a string) to 12.8 (a decimal number), and 12.8 to an integer (it will be " +
                    "rounded to 12), but you can't convert a list to a boolean or whatever the heck you are thinking about " +
                    "(just joking you're intelligent <3).";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[] {};
        }
    };

    public static final ExceptionType GLOBAL_NOT_ALLOWED = new ExceptionType() {
        @Override public String getName() {
            return "GLOBAL_NOT_ALLOWED";
        }
        @Override public String getDescription() {
            return "Global not allowed means that you can't use the \"global\" keyword where you just used it.";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[] {};
        }
    };

    public static final ExceptionType INVALID_ARGUMENTS = new ExceptionType() {
        @Override public String getName() {
            return "INVALID_ARGUMENTS";
        }
        @Override public String getDescription() {
            return "Invalid arguments happens when you don't give the right arguments to a function.";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[] {};
        }
    };

    public static final ExceptionType INVALID_OPERATOR = new ExceptionType() {
        @Override public String getName() {
            return "INVALID_OPERATOR";
        }
        @Override public String getDescription() {
            return "Invalid operator happens when the operator you are using doesn't work with the two " +
                    "values you put in, for example you can't subtract a list to a boolean (what would that mean ??)";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[] {};
        }
    };

    public static final ExceptionType INVALID_TYPE = new ExceptionType() {
        @Override public String getName() {
            return "INVALID_TYPE";
        }
        @Override public String getDescription() {
            return "Invalid type happens when the type you are using doesn't work with the function you used " +
                    "it with. This happens very often in functions, but it can also show up in iterations (for i in ...) " +
                    "or with the splat operator * on something other than a list or ** on something other than a dictionary.";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[] {};
        }
    };

    public static final ExceptionType NOT_DEFINED = new ExceptionType() {
        @Override public String getName() {
            return "NOT_DEFINED";
        }
        @Override public String getDescription() {
            return "Not defined is an error happening when you are calling a variable or a function that is " +
                    "not defined, or you are calling an element in a dictionary that doesn't exist (e.g. [=][\"something\"] ).";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[] {};
        }
    };

    public static final ExceptionType NOT_LISTABLE = new ExceptionType() {
        @Override public String getName() {
            return "NOT_LISTABLE";
        }
        @Override public String getDescription() {
            return "Not listable happens if you are using the 'in' keyword to check if an element is in a " +
                    "list/dictionary/text, but the last part is not a list (e.g. when you do something like if 10 in 12 {...}";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[] {};
        }
    };

    public static final ExceptionType NOT_OVERRIDABLE = new ExceptionType() {
        @Override public String getName() {
            return "NOT_OVERRIDABLE";
        }
        @Override public String getDescription() {
            return "Not overridable shows up when you try to create a variable but it already exists as " +
                    "function, or when you are creating a global variable but it already exists as non-global variable.";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[] {};
        }
    };

    public static final ExceptionType OUT_OF_BOUNDS = new ExceptionType() {
        @Override public String getName() {
            return "OUT_OF_BOUNDS";
        }
        @Override public String getDescription() {
            return "Out of bounds just means you called an element in a list but your index was equal or " +
                    "above the number of elements in the list (remember that list indexes start at 0), or the negative " +
                    "equivalent. For example, if you have the list [\"a\", \"b\", \"c\"], you can call elements 0 " +
                    "(-> \"a\"), 1 (-> \"b\"), 2 (-> \"c\"), or negative index -1 (-> \"c\"), -2 (-> \"b\"), -3 (-> \"a\"). " +
                    "For a list of 3 elements, these are the only 6 indexes that will not cause an OUT_OF_BOUNDS error.";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[] {};
        }
    };

    public static final ExceptionType PARSING_ERROR = new ExceptionType() {
        @Override public String getName() {
            return "PARSING_ERROR";
        }
        @Override public String getDescription() {
            return "A parsing error happens with eval() or exec(). These two functions are should not be used " +
                    "for more security, code readability and performance, but they are still here in case you found no other " +
                    "way. The code you provided to one of these function is not valid. Remember that eval() accepts only an " +
                    "expression, not full lines of code. For example, here are some expressions: log foo.bar(foo1, bar1) ; " +
                    "4 * a ; 4 > a ; 4 > a ? \"a is small\" else \"a is big\" . But these are NOT expressions: if, for, " +
                    "thread, while, try...";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[] {};
        }
    };

    public static final ExceptionType SHOULD_NOT_HAPPEN = new ExceptionType() {
        @Override public String getName() {
            return "SHOULD_NOT_HAPPEN";
        }
        @Override public String getDescription() {
            return "Hey ! You got a Should Not Happen error ! As the name states it, this error should " +
                    "never happen and it is a bug. Please report it at https://github.com/bananasmoothii/ScriptCommands/issues .";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[] {};
        }
    };

    public static final ExceptionType UNAVAILABLE_THREAD_NAME = new ExceptionType() {
        @Override public String getName() {
            return "UNAVAILABLE_THREAD_NAME";
        }
        @Override public String getDescription() {
            return "Unavailable thread name means that you wanted to make a new thread with a name " +
                    "that already exists.";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[] {};
        }
    };

    public static final ExceptionType THREAD_GROUP_ERROR = new ExceptionType() {
        @Override public String getName() {
            return "THREAD_GROUP_ERROR";
        }
        @Override public String getDescription() {
            return "A thread group error can happen either when you are trying to make a new thread " +
                    "with a group that does not exist (like `thread in \"group_that_does_not_exist\" ...`), or when you create " +
                    "a new thread group with a name that already exists (like `TODO: write this section`), or when you try " + // TODO: write these
                    "to modify a thread group that does not exist (like `TODO: write this section too`)̀.";
        }
        @Override public String[] getDocumentationLinks() {
            return new String[] {};
        }
    };
}

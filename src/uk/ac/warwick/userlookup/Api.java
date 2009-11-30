package uk.ac.warwick.userlookup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation to denote what parts of UserLookup are "public" and shouldn't change.
 */
@Target(ElementType.TYPE)
public @interface Api {

}

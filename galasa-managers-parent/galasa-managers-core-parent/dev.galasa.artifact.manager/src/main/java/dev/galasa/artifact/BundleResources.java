/*
 * Copyright contributors to the Galasa project
 */
package dev.galasa.artifact;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.galasa.framework.spi.ValidAnnotatedFields;

/**
 * Obtain a {@link IBundleResources} object for this test class
 *
 * @author Michael Baylis
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@ValidAnnotatedFields({ IBundleResources.class })
@ArtifactManagerField
public @interface BundleResources {

}

package util

/**
 * Marks elements that contain generated or unreachable code paths.
 * JaCoCo excludes classes/methods annotated with any annotation named "Generated".
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class Generated

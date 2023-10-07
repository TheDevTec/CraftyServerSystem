package me.devtec.craftyserversystem.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.meta.TypeQualifier;
import javax.annotation.meta.TypeQualifierValidator;
import javax.annotation.meta.When;

@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.METHOD })
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Nonnull {

	When when() default When.ALWAYS;

	public static class Checker implements TypeQualifierValidator<Nonnull> {
		@Override
		public When forConstantValue(final Nonnull qualifierArgument, final Object value) {
			if (value == null)
				return When.NEVER;
			return When.ALWAYS;
		}
	}
}

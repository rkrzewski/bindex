package pl.caltha.labs.ldapfilters;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.caltha.labs.ldapfilters.FilterFactory.and;
import static pl.caltha.labs.ldapfilters.FilterFactory.filter;
import static pl.caltha.labs.ldapfilters.FilterFactory.not;
import static pl.caltha.labs.ldapfilters.FilterFactory.or;
import static pl.caltha.labs.ldapfilters.FilterFactory.requirement;
import static pl.caltha.labs.ldapfilters.Operator.EQUAL;
import static pl.caltha.labs.ldapfilters.Operator.GREATER_EQ;
import static pl.caltha.labs.ldapfilters.Operator.LESS_EQ;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Version;

@RunWith(MockitoJUnitRunner.class)
public class VisitorTest {

	@Mock
	private FilterVisitor<Integer> visitor;

	@Before
	public void stubVisitor() {
		LastPlusOne lp1 = new LastPlusOne();
		when(visitor.visit(isA(StringFilter.class), argThat(lp1.matcher())))
				.thenAnswer(lp1.answer());
		when(visitor.visit(isA(LongFilter.class), argThat(lp1.matcher())))
				.thenAnswer(lp1.answer());
		when(visitor.visit(isA(DoubleFilter.class), argThat(lp1.matcher())))
				.thenAnswer(lp1.answer());
		when(visitor.visit(isA(VersionFilter.class), argThat(lp1.matcher())))
				.thenAnswer(lp1.answer());
		when(visitor.visit(isA(ListFilter.class), argThat(lp1.matcher())))
				.thenAnswer(lp1.answer());
		when(visitor.visit(isA(Not.class), argThat(lp1.matcher()))).thenAnswer(
				lp1.answer());
		when(visitor.visit(isA(Or.class), argThat(lp1.matcher()))).thenAnswer(
				lp1.answer());
		when(visitor.visit(isA(And.class), argThat(lp1.matcher()))).thenAnswer(
				lp1.answer());
		when(
				visitor.visit(isA(RequirementFilter.class),
						argThat(lp1.matcher()))).thenAnswer(lp1.answer());
	}
	
	// simple filters

	@Test
	public void testStringFilter() {
		Filter f = filter("a", Operator.EQUAL, "v");
		f.accept(visitor, 0);
		verify(visitor).visit(isA(StringFilter.class), eq(0));
	}

	@Test
	public void testLongFilter() {
		Filter f = filter("a", Operator.EQUAL, 1l);
		f.accept(visitor, 0);
		verify(visitor).visit(isA(LongFilter.class), eq(0));
	}

	@Test
	public void testDoubleFilter() {
		Filter f = filter("a", Operator.EQUAL, 1d);
		f.accept(visitor, 0);
		verify(visitor).visit(isA(DoubleFilter.class), eq(0));
	}

	@Test
	public void testVersionFilter() {
		Filter f = filter("a", Operator.EQUAL, new Version(1, 0, 0));
		f.accept(visitor, 0);
		verify(visitor).visit(isA(VersionFilter.class), eq(0));
	}

	@Test
	public void testListFilter() {
		Filter f = filter("a", Operator.EQUAL, AttributeType.LONG, 1l, 2l, 3l);
		f.accept(visitor, 0);
		verify(visitor).visit(isA(ListFilter.class), eq(0));
	}

	// compound filters

	@Test
	public void testNotFilter() {
		Filter f = not(filter("a", Operator.EQUAL, "v"));
		f.accept(visitor, 0);
		InOrder inOrder = inOrder(visitor);
		inOrder.verify(visitor).visit(isA(StringFilter.class), eq(0));
		inOrder.verify(visitor).visit(isA(Not.class), eq(1));
	}

	@Test
	public void testAndFilter() {
		Filter f = and(filter("a", Operator.EQUAL, "v"),
				filter("b", Operator.EQUAL, 1l));
		f.accept(visitor, 0);
		InOrder inOrder = inOrder(visitor);
		inOrder.verify(visitor).visit(isA(StringFilter.class), eq(0));
		inOrder.verify(visitor).visit(isA(LongFilter.class), eq(1));
		inOrder.verify(visitor).visit(isA(And.class), eq(2));
	}

	@Test
	public void testOrFilter() {
		Filter f = or(filter("a", Operator.EQUAL, "v"),
				filter("b", Operator.EQUAL, 1l));
		f.accept(visitor, 0);
		InOrder inOrder = inOrder(visitor);
		inOrder.verify(visitor).visit(isA(StringFilter.class), eq(0));
		inOrder.verify(visitor).visit(isA(LongFilter.class), eq(1));
		inOrder.verify(visitor).visit(isA(Or.class), eq(2));
	}

	@Test
	public void testRequirement() {
		Filter f = requirement(
				"osgi.wiring.package",
				and(filter("osgi.wiring.package", EQUAL, "org.osgi.framework"),
						filter("version", GREATER_EQ, new Version(1, 6, 0))));
		f.accept(visitor, 0);
		InOrder inOrder = inOrder(visitor);
		inOrder.verify(visitor).visit(isA(StringFilter.class), eq(0));
		inOrder.verify(visitor).visit(isA(VersionFilter.class), eq(1));
		inOrder.verify(visitor).visit(isA(And.class), eq(2));
		inOrder.verify(visitor).visit(isA(RequirementFilter.class), eq(3));
	}
	
	// requirements

	@Test
	public void testComplexRequirement() {
		Filter f = and(
				requirement(
						"osgi.wiring.package",
						and(filter("osgi.wiring.package", EQUAL, "javax.mail"),
								filter("version", GREATER_EQ, new Version(1, 4,
										0)),
								filter("version", LESS_EQ, new Version(2, 0, 0)))),
				requirement(
						"osgi.identity",
						filter("license", EQUAL,
								"http://www.opensource.org/licenses/EPL-1.0")));
		f.accept(visitor, 0);
		InOrder inOrder = inOrder(visitor);
		inOrder.verify(visitor).visit(isA(StringFilter.class), eq(0));
		inOrder.verify(visitor).visit(isA(VersionFilter.class), eq(1));
		inOrder.verify(visitor).visit(isA(VersionFilter.class), eq(2));
		inOrder.verify(visitor).visit(isA(And.class), eq(3));
		inOrder.verify(visitor).visit(isA(RequirementFilter.class), eq(4));
		inOrder.verify(visitor).visit(isA(StringFilter.class), eq(5));
		inOrder.verify(visitor).visit(isA(RequirementFilter.class), eq(6));
		inOrder.verify(visitor).visit(isA(And.class), eq(7));
	}

	private static class LastPlusOne {

		private int last;

		public ArgumentMatcher<Integer> matcher() {
			return new ArgumentMatcher<Integer>() {
				@Override
				public boolean matches(Object argument) {
					last = (Integer) argument;
					return true;
				}
			};
		}

		public Answer<Integer> answer() {
			return new Answer<Integer>() {
				public Integer answer(InvocationOnMock invocation)
						throws Throwable {
					return last + 1;
				}
			};
		}
	}
}
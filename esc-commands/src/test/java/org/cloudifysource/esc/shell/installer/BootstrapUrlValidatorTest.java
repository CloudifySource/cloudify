package org.cloudifysource.esc.shell.installer;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationMessageType;
import org.cloudifysource.esc.shell.installer.BootstrapUrlValidator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * The class <code>BootstrapUrlValidatorTest</code> contains tests for the class {@link <code>BootstrapUrlValidator</code>}
 * 
 * @pattern JUnit Test Case
 * 
 * @generatedBy CodePro at 10/2/13 3:54 PM
 * 
 * @author barakme
 * 
 * @version $Revision$
 */
public class BootstrapUrlValidatorTest {

	private static final int PROXY_PORT = 18080;
	private static final String HTTP_PROXY_PORT = "http.proxyPort";
	private static final String HTTP_PROXY_HOST = "http.proxyHost";

	@Test
	public void testWarningOnBadHost() throws CloudProvisioningException {
		final Cloud cloud = new Cloud();
		final ComputeTemplate template = new ComputeTemplate();
		cloud.getProvider().setCloudifyUrl("http://blabla/link");
		cloud.getCloudCompute().getTemplates().put("atemplate", template);

		final ValidationContext context = Mockito.mock(ValidationContext.class);

		final MutableBoolean found = new MutableBoolean(false);

		Mockito.doAnswer(new Answer<Void>() {

			@Override
			public Void answer(final InvocationOnMock invocation) throws Throwable {
				final Object[] args = invocation.getArguments();
				if (args[0].equals(ValidationMessageType.TOP_LEVEL_VALIDATION_MESSAGE)
						&& args[1].toString().contains("Unable to validate URL")) {
					found.setValue(true);
				}

				return null;
			}
		}).when(context).validationOngoingEvent(Matchers.any(ValidationMessageType.class), Matchers.any(String.class));

		// Mockito.doNothing().when(context).
		new BootstrapUrlValidator(cloud).validateCloudifyUrls(context);

		Assert.assertTrue("Missing validation event", found.booleanValue());

	}

	private class ProxyHandler extends AbstractHandler {

		private final AtomicInteger counter;

		public ProxyHandler(final AtomicInteger counter) {
			this.counter = counter;
		}

		@Override
		public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
				final HttpServletResponse response)
				throws IOException, ServletException {
			counter.incrementAndGet();
			throw new ServletException("Request arrived at proxy");
		}

	}

	//@Test
	public void testProxy() throws Exception {
		final Cloud cloud = new Cloud();
		final ComputeTemplate template = new ComputeTemplate();
		cloud.getProvider().setCloudifyUrl("http://blabla/bla");
		cloud.getCloudCompute().getTemplates().put("atemplate", template);

		final ValidationContext context = Mockito.mock(ValidationContext.class);
		final AtomicInteger counter = new AtomicInteger();

		final ProxyHandler handler = new ProxyHandler(counter);
		final Server httpServer = new Server(18080);

		httpServer.setHandler(handler);

		final String proxyHostBefore = System.getProperty(HTTP_PROXY_HOST);
		final String proxyPortBefore = System.getProperty(HTTP_PROXY_PORT);

		System.setProperty(HTTP_PROXY_HOST, "localhost");
		System.setProperty(HTTP_PROXY_PORT, "" + PROXY_PORT);

		try {
			httpServer.start();

			try {
				new BootstrapUrlValidator(cloud).validateCloudifyUrls(context);
				Assert.fail("Expected a validation error");
			} catch (final CloudProvisioningException e) {
				System.out.println(e);
			}

			Assert.assertEquals("Missing proxy access", 1, counter.get());

		} finally {

			try {
				httpServer.stop();
			} catch (final Exception e) {
				// ignore
			}
			if (proxyHostBefore == null) {
				System.clearProperty(HTTP_PROXY_HOST);
			} else {
				System.setProperty(HTTP_PROXY_HOST, proxyHostBefore);
			}

			if (proxyPortBefore == null) {
				System.clearProperty(HTTP_PROXY_PORT);
			} else {
				System.setProperty(HTTP_PROXY_PORT, proxyPortBefore);
			}

		}

	}
}
package eu.modernmt.rest.framework.routing;

public class TemplateException extends Exception {

	private static final long serialVersionUID = 1L;

	public final String var;

	public TemplateException(String var) {
		super();
		this.var = var;
	}

}

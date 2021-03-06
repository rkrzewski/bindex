package org.osgi.service.indexer.impl;

import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.IndexableResource;
import org.osgi.service.indexer.Namespaces;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.impl.util.BuilderImpl;
import org.osgi.service.log.LogService;

public class BlueprintAnalyzer implements ResourceAnalyzer {

	private static final String BUNDLE_BLUEPRINT_HEADER = "Bundle-Blueprint";
	
	@SuppressWarnings("unused")
	private LogService log;

	public BlueprintAnalyzer(LogService log) {
		this.log = log;
	}

	public void analyzeResource(IndexableResource resource, List<Capability> capabilities, List<Requirement> requirements) throws Exception {
		boolean blueprintEnabled = false;
		
		String header = resource.getManifest().getMainAttributes().getValue(BUNDLE_BLUEPRINT_HEADER);
		if(header != null) {
			blueprintEnabled = true;
		} else {
			List<String> children = resource.listChildren("OSGI-INF/blueprint/");
			if (children != null) {
				for (String child : children) {
					if (child.toLowerCase().endsWith(".xml")) {
						blueprintEnabled = true;
						break;
					}
				}
			}
		}
		
		if (blueprintEnabled)
			requirements.add(createRequirement());
	}

	private Requirement createRequirement() {
		Builder builder = new BuilderImpl().setNamespace(Namespaces.NS_EXTENDER);
		String filter = String.format("(&(%s=%s)(version>=1.0.0)(!(version>=2.0.0)))", Namespaces.NS_EXTENDER, Namespaces.EXTENDER_BLUEPRINT);
		builder.addDirective(Namespaces.DIRECTIVE_FILTER, filter)
			.addDirective(Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);
		return builder.buildRequirement();
	}

}

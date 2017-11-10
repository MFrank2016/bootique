package io.bootique.config.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.bootique.config.ConfigurationFactory;
import io.bootique.type.TypeRef;

import java.io.IOException;

/**
 * {@link ConfigurationFactory} based on Jackson {@link JsonNode} data
 * structure. The actual configuration can come from JSON, YAML, XML, etc.
 * 
 * @since 0.17
 */
public class JsonNodeConfigurationFactory implements ConfigurationFactory {

	private JsonNode rootNode;
	private ObjectMapper mapper;
	private TypeFactory typeFactory;

	public JsonNodeConfigurationFactory(JsonNode rootConfigNode, ObjectMapper objectMapper) {
		this.typeFactory = TypeFactory.defaultInstance();
		this.mapper = objectMapper;
		this.rootNode = rootConfigNode;
	}

	@Override
	public <T> T config(Class<T> type, String prefix) {

		JsonNode child = findChild(prefix);

		try {
			return mapper.readValue(new TreeTraversingParser(child, mapper), type);
		}
		// TODO: implement better exception handling. See ConfigurationFactory
		// in Dropwizard for inspiration
		catch (IOException e) {
			throw new RuntimeException("Error creating config", e);
		}
	}

	@Override
	public <T> T config(TypeRef<? extends T> type, String prefix) {

		JsonNode child = findChild(prefix);

		JavaType jacksonType = typeFactory.constructType(type.getType());

		try {
			return mapper.readValue(new TreeTraversingParser(child, mapper), jacksonType);
		}
		// TODO: implement better exception handling. See ConfigurationFactory
		// in Dropwizard for inspiration
		catch (IOException e) {
			throw new RuntimeException("Error creating config", e);
		}
	}

	protected JsonNode findChild(String path) {

		// assuming prefix is case-insensitive. This allows prefixes that are defined in the shell vars and nowhere
		// else...

		// TODO: this also makes YAML prefix case-insensitive, and the whole config more ambiguous, which is less than
		// ideal. Perhaps we can freeze prefix case for YAMLs by starting with a synthetic JsonNode based on the prefix
		// and then overlay CS config (YAML) only then - CI config (vars)? This will require deep refactoring. Also
		// we will need to know the type of the root JsonNode (String vs Object vs List, etc.)

		return new CiPathSegment(rootNode, path, '.').lastPathComponent().map(t -> t.getNode())
				.orElse(new ObjectNode(null));
	}

}

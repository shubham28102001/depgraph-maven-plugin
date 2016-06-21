package com.github.ferstl.depgraph.graph.style;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;
import com.github.ferstl.depgraph.dot.AttributeBuilder;
import com.github.ferstl.depgraph.graph.NodeResolution;
import com.google.common.collect.ImmutableMap;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class StyleConfiguration {

  AbstractNode defaultNode;
  Edge defaultEdge = new Edge();
  Map<String, ? extends AbstractNode> scopedNodes;
  Map<NodeResolution, Edge> edgeTypes;

  public StyleConfiguration() {
    this.defaultNode = new Box();
    this.defaultNode.defaultFont = new Font();
    this.defaultNode.defaultFont.name = "Helvetica";
    this.defaultNode.defaultFont.size = 14;
    this.defaultNode.groupIdFont = new Font();
    this.defaultNode.groupIdFont.size = 10;
    this.defaultNode.versionFont = new Font();
    this.defaultNode.versionFont.size = 10;
    this.defaultNode.scopeFont = new Font();
    this.defaultNode.scopeFont.size = 10;

    this.defaultEdge = new Edge();
    this.defaultEdge.font = new Font();
    this.defaultEdge.font.name = "Helvetica";
    this.defaultEdge.font.size = 10;

    Edge conflictEdge = new Edge();
    conflictEdge.style = "dashed";
    conflictEdge.color = "red";
    conflictEdge.font = new Font();
    conflictEdge.font.color = "red";

    Edge duplicateEdge = new Edge();
    duplicateEdge.style = "dashed";

    this.edgeTypes = ImmutableMap.of(
        NodeResolution.OMITTED_FOR_DUPLICATE, duplicateEdge,
        NodeResolution.OMITTED_FOR_CONFLICT, conflictEdge);

    this.scopedNodes = ImmutableMap.of();
  }

  public static void main(String[] args) {
    Gson gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
        .enableComplexMapKeySerialization()
        .registerTypeAdapter(NodeResolution.class, new TypeAdapter<NodeResolution>() {

          @Override
          public void write(JsonWriter out, NodeResolution value) throws IOException {
            out.value(value.name().replace('_', '-').toLowerCase());
          }

          @Override
          public NodeResolution read(JsonReader in) throws IOException {
            String value = in.nextString();
            return NodeResolution.valueOf(value.replace('-', '_').toUpperCase());
          }
        })
        .registerTypeAdapter(AbstractNode.class, new JsonDeserializer<AbstractNode>() {

          @Override
          public AbstractNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonElement type = json.getAsJsonObject().get("type");
            if (type == null) {
              throw new NullPointerException("Undefined shape type");
            }

            switch (type.getAsString()) {
              case "box":
                return context.deserialize(json, Box.class);
              case "polygon":
                return context.deserialize(json, Polygon.class);
              case "ellipse":
                return context.deserialize(json, Ellipse.class);
              default:
                throw new IllegalArgumentException("Unknown shape type: " + type.getAsString());
            }
          }
        })
        .registerTypeAdapter(AbstractNode.class, new JsonSerializer<AbstractNode>() {

          @Override
          public JsonElement serialize(AbstractNode src, Type typeOfSrc, JsonSerializationContext context) {
            if (src instanceof Box) {
              return context.serialize(src, Box.class);
            } else if (src instanceof Polygon) {
              return context.serialize(src, Polygon.class);
            } else if (src instanceof Ellipse) {
              return context.serialize(src, Ellipse.class);
            } else {
              throw new IllegalArgumentException("Unsupported shape type: " + typeOfSrc);
            }
          }
        })
        .setPrettyPrinting()
        .create();

    System.out.println(gson.toJson(new StyleConfiguration()));

    try (InputStream is = ClassLoader.getSystemResourceAsStream("style.json")) {
      StyleConfiguration config = gson.fromJson(new InputStreamReader(is), StyleConfiguration.class);
      System.err.println(gson.toJson(config));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public AttributeBuilder configureDefaultNode() {
    AttributeBuilder builder = new AttributeBuilder();
    this.defaultNode.setAttributes(builder);
    return builder;
  }

  public AttributeBuilder configureDefaultEdge() {
    AttributeBuilder builder = new AttributeBuilder();
    this.defaultEdge.setAttributes(builder);
    return builder;
  }

  public AttributeBuilder configureEdge(NodeResolution resolution) {
    Edge edge = this.edgeTypes.get(resolution);
    AttributeBuilder builder = new AttributeBuilder();
    if (edge != null) {
      edge.setAttributes(builder);
    }

    return builder;
  }

  public String renderNode(String groupId, String artifactId, String version, String scopes, String effectiveScope) {
    AbstractNode node = this.scopedNodes.containsKey(effectiveScope) ? this.scopedNodes.get(effectiveScope) : this.defaultNode;
    return node.renderLabel(groupId, artifactId, version, scopes);
  }
}

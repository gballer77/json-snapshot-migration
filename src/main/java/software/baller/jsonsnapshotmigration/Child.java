package software.baller.jsonsnapshotmigration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="jsonType", defaultImpl = ChildV1.class)
@JsonSubTypes({@JsonSubTypes.Type(name = "v1", value = ChildV1.class)})
public class Child {
}

package software.baller.jsonsnapshotmigration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="jsonType", defaultImpl = ChildV1.class)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "v1", value = ChildV1.class),
        @JsonSubTypes.Type(name = "v2", value = ChildV2.class)
})
public class Child {
    ChildV2 get() {
        if(this instanceof ChildV1 c) {
            return ChildV2.builder()
                    .id(c.id)
                    .favoriteColor(c.favoriteColor)
                    .name(c.name)
                    .age(null)
                    .build();
        } else if(this instanceof ChildV2) {
            return (ChildV2) this;
        } else {
            throw new IllegalArgumentException("Unknown child type: " + this.getClass().getName());
        }
    }
}

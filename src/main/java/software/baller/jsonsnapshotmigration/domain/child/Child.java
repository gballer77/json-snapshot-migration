package software.baller.jsonsnapshotmigration.domain.child;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="jsonType", defaultImpl = ChildV1.class)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "v1", value = ChildV1.class),
        @JsonSubTypes.Type(name = "v2", value = ChildV2.class)
})
public class Child {
    ChildMapper mapper = Mappers.getMapper(ChildMapper.class);

    public ChildV2 get() throws IllegalArgumentException{
        if(this instanceof ChildV1 c) {
            return mapper.v1ToV2(c);
        } else if(this instanceof ChildV2) {
            return (ChildV2) this;
        } else {
            throw new IllegalArgumentException("Unknown child type: " + this.getClass().getName());
        }
    }

    // If this function is named `getV1` it will cause issues with Jackson Marshalling
    @Deprecated(since="2024-05-29")
    public ChildV1 v1() throws IllegalArgumentException{
        if(this instanceof ChildV1) {
            return (ChildV1) this;
        } else if(this instanceof ChildV2 v2) {
            return mapper.v2ToV1(v2);
        } else {
            throw new IllegalArgumentException("Unknown child type: " + this.getClass().getName());
        }
    }

    @Mapper
    interface ChildMapper {
        @Mapping(target = "age", ignore = true)
        ChildV2 v1ToV2(ChildV1 c);

        ChildV1 v2ToV1(ChildV2 c);
    }
}

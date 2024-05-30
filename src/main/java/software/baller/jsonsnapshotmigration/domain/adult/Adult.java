package software.baller.jsonsnapshotmigration.domain.adult;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="jsonType", defaultImpl = AdultV1.class)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "v1", value = AdultV1.class),
        @JsonSubTypes.Type(name = "v2", value = AdultV2.class)
})
public class Adult {
    Adult.AdultMapper mapper = Mappers.getMapper(Adult.AdultMapper.class);

    public AdultV2 get() throws IllegalArgumentException{
        if(this instanceof AdultV1 a) {
            return mapper.v1ToV2(a);
        } else if(this instanceof AdultV2) {
            return (AdultV2) this;
        } else {
            throw new IllegalArgumentException("Unknown child type: " + this.getClass().getName());
        }
    }

    // If this function is named `getV1` it will cause issues with Jackson Marshalling
    @Deprecated(since="2024-05-29")
    public AdultV1 v1() throws IllegalArgumentException {
        if(this instanceof AdultV1) {
            return (AdultV1) this;
        } else if(this instanceof AdultV2 v2) {
            return mapper.v2ToV1(v2);
        } else {
            throw new IllegalArgumentException("Unknown child type: " + this.getClass().getName());
        }
    }

    @Mapper
    interface AdultMapper {
        @Named("yearsToMonths")
        default Integer yearsToMonths(Integer years) {
            return years * 12;
        }

        @Named("monthsToYears")
        default Integer monthsToYears(Integer months) {
            return months / 12;
        }

        @Mapping(target = "ageInMonths", source = "age", qualifiedByName = "yearsToMonths")
        AdultV2 v1ToV2(AdultV1 v1);

        @Mapping(target = "age", source = "ageInMonths", qualifiedByName = "monthsToYears")
        @Deprecated(since="2024-05-29")
        AdultV1 v2ToV1(AdultV2 v2);
    }
}

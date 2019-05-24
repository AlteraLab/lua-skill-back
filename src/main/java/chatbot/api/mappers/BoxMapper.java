package chatbot.api.mappers;

import chatbot.api.build.domain.BoxDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface BoxMapper {

    BoxDto getEntryBoxByUsrDfinName(@Param("usrDfinName") String userDefinedName);

    BoxDto getBoxByBoxId(@Param("boxId") Long boxId);
}
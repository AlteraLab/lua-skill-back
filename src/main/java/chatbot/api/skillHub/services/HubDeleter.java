package chatbot.api.skillHub.services;

import chatbot.api.common.domain.ResponseDto;
import chatbot.api.mappers.HubMapper;
import chatbot.api.mappers.RoleMapper;
import chatbot.api.role.domain.RoleDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static chatbot.api.skillHub.utils.HubConstants.*;

@Service
@Slf4j
@AllArgsConstructor
public class HubDeleter {

    //@Autowired
    private HubMapper hubMapper;

    //@Autowired
    private RoleMapper roleMapper;


    // 나중에 허브에 대한 모듈 테이블이 자식 테이블로 생성될 시 자식 테이블으 모듈들도 제거해주는 코드 작성
    @Transactional
    public ResponseDto explicitDeleter(RoleDto role) {

        ResponseDto responseDto = new ResponseDto().builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();


        try {

            // 허브에 종속된 모듈 제거 메서드 실행
            // ...

            // skillHubUserMapper.deleterHubUser
            responseDto.setMsg(FAIL_MSG_EXPLICIT_DEL_AT_QUERY_ABOUT_ROLL);
            roleMapper.deleteRole(role);

            // skillHubMapper.deleterUser
            responseDto.setMsg(FAIL_MSG_EXPLICIT_DEL_AT_QUERY_ABOUT_HUB);
            hubMapper.deleteHub(role.getHubSeq());

            responseDto.setMsg(SUCCESS_MSG_EXPLICIT_DEL);
            responseDto.setStatus(HttpStatus.OK);

        } catch (Exception e) {
            log.debug(EXCEPTION_MSG_DURING_DELETER);
            e.printStackTrace();

        } finally {
            return responseDto;
        }
    }
}
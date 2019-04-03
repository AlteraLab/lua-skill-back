package chatbot.api.skillHub;

import chatbot.api.mappers.RoleMapper;
import chatbot.api.role.domain.RoleDto;
import chatbot.api.common.domain.ResponseDto;
import chatbot.api.common.security.UserPrincipal;
import chatbot.api.mappers.HubMapper;
import chatbot.api.skillHub.domain.*;
import chatbot.api.skillHub.services.EditHub;
import chatbot.api.skillHub.services.HubDeleter;
import chatbot.api.skillHub.services.HubGetter;
import chatbot.api.skillHub.services.HubRegister;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static chatbot.api.role.utils.RoleConstants.*;
import static chatbot.api.skillHub.utils.HubConstants.*;

@SuppressWarnings("ALL")
@RestController
@NoArgsConstructor
@Slf4j
public class HubController {

    @Autowired
    private HubRegister hubRegister;

    @Autowired
    private HubDeleter hubDeleter;

    @Autowired
    private HubGetter hubGetter;

    @Autowired
    private EditHub editHub;

    @Autowired
    private RestTemplate deleteIpInHub;

    @Autowired
    private HubMapper hubMapper;

    @Autowired
    private RoleMapper roleMapper;



    // 특정 유저가 컨트롤 할 수 있는 허브 모두 조회하는 메소드
    // 이거 왜있는지 모르겠음... 나중에 교준이한테 물어보기..
    @GetMapping("/hub")
    public ResponseDto getSpecificUserHub(@AuthenticationPrincipal UserPrincipal userPrincipal){

        Optional<HubInfoDto> hubs = hubMapper.getUserHub(userPrincipal.getId());

        return ResponseDto.builder()
                .msg("success")
                .status(HttpStatus.OK)
                .data(hubs)
                .build();
    }



    // hub를 삭제해주는 메소드, 나중에 허브에 대한 모듈 테이블이 자식 테이블로 생성될 시 자식 테이블으 모듈들도 제거해주는 코드 작성
    // 추후에 허브에 모듈이 붙으면 모듈들에 데이터를 제거하는 코드도 추가해야함.
    @DeleteMapping("/hub")
    public ResponseDto deleteHub(//@PathVariable("userId") Long userId,
                                 @AuthenticationPrincipal UserPrincipal userPrincipal,
                                 @RequestBody HubInfoVo hubInfoVo) {

        RoleDto role = new RoleDto().builder()
                //.userSeq(userId)
                .userSeq(userPrincipal.getId())
                .hubSeq(hubInfoVo.getHubSequence())
                .build();

        ResponseDto responseDto = new ResponseDto().builder()
                .status(HttpStatus.ACCEPTED)
                .build();


        HubInfoDto hub = hubMapper.getHubInfo(hubInfoVo.getHubSequence());
        if(hub == null) {
            responseDto.setMsg(FAIL_MSG_DELETE_HUB_BECAUSE_NO_EXIST);
            return responseDto;
        }

        // ROLE_USER   VS   ROLE_ADMIN
//        if(!userId.equals(hub.getAdminSeq())) {     // ROLE_USER
        if(!userPrincipal.getId().equals(hub.getAdminSeq())) {     // ROLE_USER

            // 1. 해당 유저가 ROLE_USER 인지 확인
            //RoleDto roleUser = roleMapper.getRoleInfo(hubInfoVo.getHubSequence(), userId);
            RoleDto roleUser = roleMapper.getRoleInfo(hubInfoVo.getHubSequence(), userPrincipal.getId());
            if(roleUser == null) {
                responseDto.setMsg(FAIL_MSG_NO_ROLE_USER_AND_ROLE_ADMIN);
                responseDto.setStatus(HttpStatus.NO_CONTENT);
                return responseDto;
            }

            return hubDeleter.explicitDeleterByUser(role);
            // 2. 한 번 더 확인 (만약 사용 가능하다면, 허브 사용 명단에서 유저를 제거)
     /*       if(roleUser.getRole().equals(ROLE_USER)) {
                return hubDeleter.explicitDeleterByUser(role);
            } else {
                responseDto.setMsg(FAIL_MSG);
                return responseDto;
            }*/

        } else {     // ROLE_ADMIN
            // 허브 서버로 db에 저장된 ip 정보 말소 요청
            String url = "http://" + hub.getExternalIp() + "/" + hub.getExternalPort() + "/ip";
            ResponseEntity<String> resultAboutDelIp = deleteIpInHub.exchange(url, HttpMethod.DELETE, null, String.class);

            if(resultAboutDelIp.equals("fail")) {
                responseDto.setMsg(FAIL_MSG_DELETE_HUB_BECAUSE_FAIL_RESTTEMPLATE);
                responseDto.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                return responseDto;
            }

            // 나중에 허브에 대한 모듈 테이블이 자식 테이블로 생성될 시 자식 테이블을 모듈들도 제거해주는 코드 작성
            return hubDeleter.explicitDeleterByAdmin(role);
        }
    }



    // 허브 최초 등록 순서 : hub 등록 -> hub_user 등록
    /*
    http://localhost:8888/user?data=002
    @PostMapping("user")
    public @ResponseBody item getitem(@RequestParam("data") String itemid){
        item i = itemDao.findOne(itemid);
        String itemname = i.getItemname();
        String price = i.getPrice();
        return i;
    }
    */
    //@PostMapping("/hub/{userId}")
    @PostMapping("/hub")
    public ResponseDto registHub(//@RequestBody HubInfoVo hubInfoVo
                                 //@PathVariable("userId") Long userId,
                                 @AuthenticationPrincipal UserPrincipal userPrincipal,
                                 @RequestParam("hubName") String  hubName,
                                 @RequestParam("externalIp") String  externalIp,
                                 @RequestParam("internalIp") String  internalIp,
                                 @RequestParam("externalPort") int externalPort,
                                 @RequestParam("internalPort") int internalPort,
                                 @RequestParam("macAddr") String  macAddress) {

        HubInfoDto hub = null;

        hub = HubInfoDto.builder()
                /*.hubName(hubInfoVo.getHubName())
                .externalIp(hubInfoVo.getExternalIp())
                .externalPort(hubInfoVo.getExternalPort())
                .internalIp(hubInfoVo.getInternalIp())
                .internalPort(hubInfoVo.getInternalPort())*/
                .hubName(hubName)
                .externalIp(externalIp)
                .externalPort(externalPort)
                .internalIp(internalIp)
                .internalPort(internalPort)
                .beforeIp(null)
                // Mac Address
                .lastUsedTime(Timestamp.valueOf(LocalDateTime.now()))
                .updatedAt(Timestamp.valueOf(LocalDateTime.now()))
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .state(true)
                .adminSeq(userPrincipal.getId())
                //.adminSeq(userId)
                .build();



        // not yet set hubSeq
        RoleDto role = RoleDto.builder()
                //.userSeq(userId)    // userPrincipal.getId();
                .userSeq(userPrincipal.getId())    // userPrincipal.getId();
                .role(ROLE_ADMIN)
                .build();

        // log
        log.info(role.toString());
        log.info(hub.toString());

        // 정상적으로 저장시 허브 서버에 "success"를 전송한다.
        // 정보를 받은 허브 서버는 react app으로 success status 전달한 이후에 허브는 사설 ip 저장 -> station 모드 실행
        return hubRegister.register(hub, role);
    }



    // hub edit, admin만 수행 가능
    @PutMapping("/hub")
    public ResponseDto editHub(//@PathVariable("userId") Long userId,
                               @AuthenticationPrincipal UserPrincipal userPrincipal,
                               @RequestBody HubInfoVo hubInfoVo) {

        ResponseDto responseDto = new ResponseDto().builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();

        log.info(hubInfoVo.toString());

        responseDto.setMsg(FAIL_MSG_NO_EXIST_HUB);
        HubInfoDto hub = hubMapper.getHubInfo(hubInfoVo.getHubSequence());
        if(hub == null)                 return responseDto;

        log.info(hub.toString());

        responseDto.setMsg(FAIL_MSG_TO_EDIT_HUB_BECAUSE_NO_ADMIN);
        if(userPrincipal.getId() != hub.getAdminSeq()) return responseDto;
        //if(userId != hub.getAdminSeq()) return responseDto;

        return editHub.editer(hubInfoVo);
    }



    // UPnP 수행 이후, 할당 받은 Ip가 이전 Ip와 다르다면 스킬 서버로 데이터를 전송해서
    // Ip 수정 실시
    @PutMapping("/hub/upnpIp")
    public ResponseDto updateUpnpIp(@RequestBody HubInfoVo hub) {

        log.info(hub.toString());

        return ResponseDto.builder()
                .msg("UPNP IP")
                .status(HttpStatus.OK)
                .build();
    }



    // 일단 만들었는데 안쓸수도 있음
    // get hubInfo<Long == hubSeq, String == hubName> by adminId, 수정 필요할듯...
    // 사용자가 허브 조회를 누르면 hubSeq와 hubName을 화면에 뿌려주는 메소드
    @GetMapping("/hubInfo/SeqAndName")
    public ResponseDto getHubsSeqAndNameByAdminId(@AuthenticationPrincipal UserPrincipal userPrincipal) {

        // get hubs by adminId
        List<HubTableVo> hubInfoList = hubGetter.getHubsInfoByadminId(userPrincipal.getId());
        if(hubInfoList == null) {
            return ResponseDto.builder()
                    .msg(FAIL_MSG_NO_HUB_REGISTED_AS_ADMIN)
                    .status(HttpStatus.OK)
                    .data(null)
                    .build();
        }

        // set hubsSeqAndName,  <Long == hubSeq, String == hubName>
        Map<Long, String> hubsSeqAndName = new HashMap<Long, String>();
        for(int i = 0; i < hubInfoList.size(); i++) {
            hubsSeqAndName.put(hubInfoList.get(i).getHub_sequence(), hubInfoList.get(i).getHub_name());
        }

        // return <Long == hubSeqs, String == hubNames>
        // react app 에서는 hubsSeqAndName을 파싱해서 화면에 뿌려주는 코드 작성 필요
        return ResponseDto.builder()
                .msg(SUCCESS_MSG_GET_HUBS_SEQ_AND_NAME)
                .status(HttpStatus.OK)
                .data(hubsSeqAndName)
                .build();
    }
}
package chatbot.api.textbox.services;

import chatbot.api.common.services.RestTemplateService;
import chatbot.api.textbox.domain.*;
import chatbot.api.textbox.domain.datamodel.KeySetListDTO;
import chatbot.api.textbox.domain.path.Path;
import chatbot.api.textbox.domain.reservation.ReservationListDTO;
import chatbot.api.textbox.domain.textboxdata.BoxDTO;
import chatbot.api.textbox.domain.textboxdata.DerivationDTO;
import chatbot.api.textbox.repository.BuildRepository;
import chatbot.api.common.domain.kakao.openbuilder.responseVer2.ResponseVerTwoDTO;
import chatbot.api.common.services.KakaoBasicCardService;
import chatbot.api.common.services.KakaoSimpleTextService;
import chatbot.api.mappers.*;
import chatbot.api.skillhub.domain.HubInfoDTO;
import chatbot.api.textbox.repository.ReservationListRepository;
import chatbot.api.user.domain.UserInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.sql.Array;
import java.sql.Timestamp;
import java.util.ArrayList;

@Service
@Slf4j
public class TextBoxResponseService {

    @Autowired
    private HubMapper hubmapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RestTemplateService restTemplateService;

    @Autowired
    private KakaoBasicCardService kakaoBasicCardService;

    @Autowired
    private KakaoSimpleTextService kakaoSimpleTextService;

    @Autowired
    private BuildRepository buildRepository;

    @Autowired
    private BuildSaveService buildSaveService;

    @Autowired
    private BuildActionService buildActionService;

    @Autowired
    private BuildAllocaterService buildAllocaterService;

    @Autowired
    private ReservationListRepository reservationListRepository;


    public ResponseVerTwoDTO responserHubsBox(String providerId) {
        log.info("================== ResponserHubs ==================");

        if (providerId == null) return kakaoBasicCardService.responserRequestPreSignUp();
        else buildSaveService.saverProviderId(providerId);

        UserInfoDto user = userMapper.getUserByProviderId(providerId).get();

        ArrayList<HubInfoDTO> hubs = hubmapper.getUserHubsByUserId(user.getUserId());
        log.info("Hub List -> " + hubs);
        buildSaveService.saverHubs(providerId, hubs);

        if (hubs == null) {
            return kakaoSimpleTextService.responserShortMsg("사용 가능한 허브가 없습니다.\n허브를 등록해주세요.");
        } else if (hubs.size() == 1) {
            buildSaveService.saverPathAboutAddr(
                    providerId,
                    Path.builder()
                            .externalIp(hubs.get(0).getExternalIp())
                            .externalPort(hubs.get(0).getExternalPort())
                            .build());

            try {
                if (buildActionService.actionRequestAndSaverAboutHrdwr(providerId)) {
                    return kakaoSimpleTextService.responserShortMsg("허브와 연결된 장비가 없습니다.");
                }
            } catch(ResourceAccessException e) { // 나중에 어떠한 이셉션이 나는지 자세히 체크해보기
                return kakaoSimpleTextService.responserShortMsg("허브 연결 상태를 확인해주세요.");
            }
            return kakaoSimpleTextService.makerHrdwrsCard(providerId);
        } else {
            return kakaoSimpleTextService.makerHubsCard(hubs);
        }
    }


    // 사용자가 발화한 허브 번호를 파싱해서 매개변수로 받는다.
    public ResponseVerTwoDTO responserHrdwrsBox(String providerId, int hubSeq) {
        log.info("================== Responser Hrdwrs ==================");
        Build reBuild = buildRepository.find(providerId);
        buildSaveService.saverPathAboutAddr(
                providerId,
                Path.builder()
                        .externalIp(reBuild.getHubs().get(hubSeq - 1).getExplicitIp())
                        .externalPort(reBuild.getHubs().get(hubSeq - 1).getExplicitPort())
                        .build());

        try {
            if (buildActionService.actionRequestAndSaverAboutHrdwr(providerId)) {
                return kakaoSimpleTextService.responserShortMsg("허브와 연결된 장비가 없습니다.");
            }
        } catch(ResourceAccessException e) { // 나중에 어떠한 이셉션이 나는지 자세히 체크해보기
            return kakaoSimpleTextService.responserShortMsg("허브 연결 상태를 확인해주세요.");
        }

        return kakaoSimpleTextService.makerHrdwrsCard(providerId);
    }


    public ResponseVerTwoDTO responserEntryBox(String providerId, Integer hrdwrSeq) {
        log.info("================== Responser EntryBox 시작 ==================");
        if(hrdwrSeq != null) {
            buildSaveService.saverSelectedHrdwr(providerId, hrdwrSeq);// 선택된 하드웨어 저장
            buildSaveService.saverPathAboutMac(providerId);           // 하드웨어 맥주소 저장
        }
        buildSaveService.saverMultipleData(providerId);           // 데이터를 빌드하는데 필요한 boxs, btns, derivations, dataModels, stateRules 저장
        buildSaveService.initCmdListWhenEntry(providerId);        // When Create Entry, cmdList 초기화(객체 할당)
        buildSaveService.initCurBoxWhenEntry(providerId);         // When Create Entry Box, 엔트리 박스(현재 박스) 구하기
        buildSaveService.initCurBtns(providerId);                 // 현재 박스 안에 존재하는 버튼들 초기화, 인덱스 값 +1
        buildSaveService.initHControlBlocks(providerId);
        return kakaoSimpleTextService.makerEntryAndControlCard(providerId); // 카드 만들기
    }


    // 1.
    // Entry -> (End Or Control),
    public ResponseVerTwoDTO responserEndBoxFrEntry(String providerId, Integer btnIdx) {
        log.info("================== Responser End Box From Entry 시작 ==================");
        // 1. 사용자가 선택한 버튼을 저장해야 한다
        //      1-1. 사용자가 선택한 버튼의 버튼 타입을 저장해야 한다
        //      1-2. 사용자가 선택한 버튼의 이벤트 코드를 저장해야 한다
        buildSaveService.initHControlBlocksToNull(providerId);
        buildSaveService.saverSelectedBtn(providerId, btnIdx);
        buildSaveService.saverCurBoxWhenEndFrEntry(providerId);
        return buildAllocaterService.allocaterResponseVerTwoDtoByExistLowerBox(providerId);
    }


    // 2.
    // Entry -> Time -> Dynamic, 사용자에게 동적 입력 받는 박스를 보여줘야 할 차례임
    public ResponseVerTwoDTO responserDynamicBoxFrTimeFrEntry(String providerId, Integer btnIdx, Timestamp timestamp) {
        log.info("================== Responser Dynamic Box From Time From Entry 시작 ==================");
        // 1. 사용자가 선택한 버튼을 저장해야 한다
        //      1-1. 사용자가 선택한 버튼의 버튼 타입을 저장해야 한다
        //      1-2. 사용자가 선택한 버튼의 이벤트 코드를 저장해야 한다
        // 2. timestamp 를 저장해야 한다
        buildSaveService.initHControlBlocksToNull(providerId);
        buildSaveService.saverSelectedBtn(providerId, btnIdx);
        buildSaveService.initAdditional(providerId);
        buildSaveService.saverTimeStamp(providerId, timestamp);

        // 입력을 받을 동적 박스를 사용자에게 보여저야 한다
        return kakaoSimpleTextService.makerDynamicCardWhenDynamicFrTimeFrEntry(providerId);
    }
    // Entry -> Time -> Dynamic -> (End Or Control), 하위 박스가 있다면 Control Box, 하위 박스가 없다면 End Box 를 보여줘야할 차례
    public ResponseVerTwoDTO responserEndBoxFrDynamicFrTimeFrEntry(String providerId, Long dynamicValue) {
        log.info("================== Responser End Box From Dynamic From Time From Entry 시작 ==================");
        // 1. 동적 입력 데이터를 Additonal 에 추가해야 한다
        // 2. 하위 박스가 있는지 없는지 체크한다
        //      2-1. 하위- 박스가 없다면 -> End Box 라는 의미이니까, 사용자에게 명령을 전달할거냐고 묻는다
        //      2-2. 하위 박스가 있다면 -> End Box 가 아니라는 의미이니까, Entry Box 때 했던것과 비슷한 작업을 수행한다
        buildSaveService.saverDynamicValue(providerId, dynamicValue);
        buildSaveService.saverCurBoxWhenEndFrDynamicFrTimeFrEntry(providerId); // curBox를 null 혹은 control box로 초기화 시킴.
        return buildAllocaterService.allocaterResponseVerTwoDtoByExistLowerBox(providerId);
    }


    // 3.
    // Entry -> _Dynamic, 사용자에게 동적 입력 받는 박스를 보여줘야 할 차례임
    public ResponseVerTwoDTO responserUDynamicBoxFrEntry(String providerId, Integer btnIdx) {
        log.info("================== Responser _Dynamic Box From Entry 시작 ==================");
        // 1. 사용자가 선택한 버튼을 저장해야 한다
        //      1-1. 사용자가 선택한 버튼의 버튼 타입을 저장해야 한다
        //      1-2. 사용자가 선택한 버튼의 이벤트 코드를 저장해야 한다
        buildSaveService.initHControlBlocksToNull(providerId);
        buildSaveService.saverSelectedBtn(providerId, btnIdx);

        // 입력을 받을 동적 박스를 사용자에게 보여줘야 한다
        return kakaoSimpleTextService.makerDynamicCardWhenUDynamicFrEntry(providerId);
    }
    // Entry -> _Dynamic -> Time -> (End Or Control), 하위 박스가 있다면 Control Box, 하위 박스가 없다면 End Box 를 보여줘야할 차례
    public ResponseVerTwoDTO responserEndBoxFrTimeFrUDynamicFrEntry(String providerId, Long dynamicValue, Timestamp timeStamp) {
        log.info("================== Responser End Box From Time From _Dynamic Box From Entry 시작 ==================");
        // 1. timestamp 를 저장해야 한다
        // 2. 동적 입력 데이터를 저장해야 한다.
        // 3. 하위 박스가 있는지 없는지 체크한다
        //      3-1. 하위 박스가 없다면 -> End Box 라는 의미이니까, 사용자에게 명령을 전달할거냐고 묻는다
        //      3-2. 하위 박스가 있다면 -> End Box 가 아니라는 의미이니까, Entry Box 때 했던것과 비슷한 작업을 수행한다

        // 현재 curBox 상태가 이전의 박스인 Dynamic Box 에 위치한 상태임
        buildSaveService.saverCurBoxWhenEndFrTimeFrUDynamicFrEntry(providerId);

        buildSaveService.initAdditional(providerId);
        buildSaveService.saverTimeStamp(providerId, timeStamp);
        buildSaveService.saverDynamicValue(providerId, dynamicValue);

        return buildAllocaterService.allocaterResponseVerTwoDtoByExistLowerBox(providerId);
    }


    // 4.
    // Entry -> Time -> (End Or Control), 하위 박스가 있다면 Control Box, 하위 박스가 없다면 End Box 를 보내줘야할 차례
    public ResponseVerTwoDTO responserEndBoxFrTimeFrEntry(String providerId, Integer btnIdx, Timestamp timestamp) {
        // 1. 사용자가 선택한 버튼을 저장해야 한다
        //      1-1. 사용자가 선택한 버튼의 버튼 타입을 저장해야 한다
        //      1-2. 사용자가 선택한 버튼의 이벤트 코드를 저장해야 한다
        // 2. timestamp 를 저장해야 한다
        // 3. 하위 박스가 있는지 없는지 체크한다
        //      3-1. 하위 박스가 없다면 -> End Box 라는 의미이니까, 사용자에게 명령을 전달할거냐고 묻는다
        //      3-2. 하위 박스가 있다면 -> End Box 가 아니라는 의미이니까, Entry Box 때 했던것과 비슷한 작업을 수행한다

        log.info("================== Responser End Box From Time From Entry 시작 ==================");

        buildSaveService.initHControlBlocksToNull(providerId);
        buildSaveService.saverSelectedBtn(providerId, btnIdx);
        buildSaveService.initAdditional(providerId);
        buildSaveService.saverTimeStamp(providerId, timestamp);

        // CurBox 가 Entry Box 이기 때문에 두 단계 아래 박스로 조정해야함
        buildSaveService.saverCurBoxWhenEndFrTimeFrEntry(providerId);

        return buildAllocaterService.allocaterResponseVerTwoDtoByExistLowerBox(providerId);
    }


    // 5.
    // Entry -> Dynamic, 사용자에게 동적 박스를 보여줘야할 차례임
    public ResponseVerTwoDTO responserDynamicBoxFrEntry(String providerId, Integer btnIdx) {
        // 1. 사용자가 선택한 버튼을 저장해야 한다
        //      1-1. 사용자가 선택한 버튼의 버튼 타입을 저장해야 한다
        //      1-2. 사용자가 선택한 버튼의 이벤트 코드를 저장해야 한다
        // 2. Cur Box 를 조정해야 한다

        log.info("================== Responser Dynamic Box From Entry 시작 ==================");

        buildSaveService.initHControlBlocksToNull(providerId);
        buildSaveService.saverSelectedBtn(providerId, btnIdx);
        buildSaveService.initAdditional(providerId);

        return kakaoSimpleTextService.makerDynamicCardWhenDynamicFrEntry(providerId);
    }
    // Entry -> Dynamic -> (End Or Control), 하위 박스가 있다면 Control Box, 하위 박스가 없다면 End Box 를 보내줘야할 차례
    public ResponseVerTwoDTO responserEndBoxFrDynamicFrEntry(String providerId, Long dynamicValue) {
        // 1. 동적 입력 데이터를 저장해야 한다
        // 2. curBox 를 조정해야 한다
        // 3. 하위 박스가 있는지 없는지 체크한다
        //      3-1. 하위 박스가 없다면 -> End Box 라는 의미이니까, 사용자에게 명령을 전달할거냐고 묻는다
        //      3-2. 하위 박스가 있다면 -> End Box 가 아니라는 의미이니까, Entry Box 때 했던것과 비슷한 작업을 수행한다

        log.info("================== Responser End Box From Dynamic From Entry 시작 ==================");

        buildSaveService.initAdditional(providerId);
        buildSaveService.saverDynamicValue(providerId, dynamicValue);

        // CurBox 가 Dynamic Box 이기 때문에 한 단계 아래 박스로 조정해야함
        buildSaveService.saverCurBoxWhenEndFrDynamicFrEntry(providerId);

        return buildAllocaterService.allocaterResponseVerTwoDtoByExistLowerBox(providerId);
    }


    public ResponseVerTwoDTO responserIntervalBox(String providerId) {
        log.info("================== Responser Interval Box 시작 ==================");
        Build reBuild = buildRepository.find(providerId);
        ArrayList<DerivationDTO> derivations = reBuild.getDerivations();
        BoxDTO curBox = reBuild.getCurBox();
        BoxDTO lowerBox = null;

        for(DerivationDTO tempDerivation : derivations) {
            if(tempDerivation.getUpperBoxId() == curBox.getBoxId()
                    && tempDerivation.getBtnCode() == reBuild.getSelectedBtn().getBtnCode()) {
                lowerBox = buildAllocaterService.allocateBoxByBoxId(providerId, tempDerivation.getLowerBoxId());
            }
        }
        reBuild.setCurBox(lowerBox);
        buildRepository.update(reBuild);

        return kakaoSimpleTextService.makerIntervalCard();
    }


    public ResponseVerTwoDTO responserReservationListBox(String providerId) {
        log.info("================== Responser Reservation List Box 시작 ==================");
        ReservationListDTO reservationList = restTemplateService.requestReservationList(providerId);
        // redis 에 데이터 저장
        reservationListRepository.save(providerId, reservationList);
        // 장치에 대한 예약 목록 박스 만들어서 반환
        return kakaoSimpleTextService.makerReservationListCard(reservationList);
    }


    public ResponseVerTwoDTO responserSensingBox(String providerId, Integer btnIdx) {
        log.info("================== Responser Sensing Box 시작 ==================");
        KeySetListDTO keySet = restTemplateService.requestKeySets(providerId, '1'); // 센싱 key 조회
        buildSaveService.saverCurBoxWhenLookUpSensingAndDeviceInfo(providerId, btnIdx);
        return kakaoSimpleTextService.makerLookUpDataCard(providerId, keySet);
    }


    public ResponseVerTwoDTO responserDevInfoBox(String providerId, Integer btnIdx) {
        log.info("================== Responser Device Info Box 시작 ==================");
        KeySetListDTO keySet = restTemplateService.requestKeySets(providerId, '0'); // 디바이스 key 조회
        buildSaveService.saverCurBoxWhenLookUpSensingAndDeviceInfo(providerId, btnIdx);
        return kakaoSimpleTextService.makerLookUpDataCard(providerId, keySet);
    }
}
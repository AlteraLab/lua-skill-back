package chatbot.api.order.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelectCmdOrder implements Serializable {

    private int cmdCode; // 전달할 명령 코드 번호

    private int data;    // 아무 의미 없는 코드, 시간 차이, 직접 입력, 버튼 코드
}
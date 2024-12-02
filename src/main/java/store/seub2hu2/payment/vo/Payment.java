package store.seub2hu2.payment.vo;

import lombok.*;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Payment {
    private int no;
    private String id;
    private String userId;
    private String method;
    private int price;
    private int amount;
    private String type;
    private String status;
    private String refund;
    private Date payCreatedDate;
    private Date payUpdatedDate;

}

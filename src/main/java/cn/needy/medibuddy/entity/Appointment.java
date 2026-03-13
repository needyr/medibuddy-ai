package cn.needy.medibuddy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-05 10:38
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Long id;             // 主键ID
    private String username;     // 用户名
    private String idCard;       // 身份证号
    private String phone;        // 手机号
    private String department;   // 科室
    private LocalDate date;      // 就诊日期（建议后续改为 LocalDate）
    private LocalTime time;      // 就诊时间（建议后续改为 LocalTime）
    private String doctorName;   // 医生姓名
}

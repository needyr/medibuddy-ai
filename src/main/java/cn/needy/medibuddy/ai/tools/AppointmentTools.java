package cn.needy.medibuddy.tools;

import cn.needy.medibuddy.entity.Appointment;
import cn.needy.medibuddy.service.AppointmentService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-05 10:56
 **/

@Component
@RequiredArgsConstructor
public class AppointmentTools {
    private final AppointmentService appointmentService;

    @Tool(
            value = "根据科室名称、日期、时间和医生名称查询是否有号源。只有当科室、日期、时间完整时才可查询，医生名称可选。")
    public boolean queryDepartment(@P(value = "科室名称") String department,
                                  @P(value = "日期") String date,
                                  @P(value = "时间") String time,
                                  @P(value = "医生名称", required = false) String doctorName){
        System.out.println("查询是否有号源,科室名称" + department + "日期" + date + "时间" + time + "医生名称" + doctorName);
        if (isBlank(department) || isBlank(date) || isBlank(time)) {
            return false;
        }
        // todo 查询真实号源数据
        return true;
    }

    @Tool(
            name = "预约挂号",
            value="根据预约信息创建挂号记录。调用前应先完成号源查询，并且只有在用户已确认预约、姓名、手机号、身份证号、日期、时间等必要信息完整时才调用。")
    public String bookAppointment(Appointment appointment){
        if (appointment == null || isIncompleteForBooking(appointment)) {
            return "预约信息不完整，请补充姓名、手机号、身份证号、日期和时间";
        }
        if (!queryDepartment(appointment.getDepartment(), String.valueOf(appointment.getDate()), String.valueOf(appointment.getTime()), appointment.getDoctorName())) {
            return "当前号源不可预约，请重新选择科室、日期或时间";
        }
        Appointment existAppointment = appointmentService.getOne(appointment);
        if (existAppointment != null) {
            return "您已经预约过该号源，请勿重复预约";
        }

        appointment.setId(null);
        boolean result = appointmentService.save(appointment);
        if (result) {
            return "预约成功：" + appointment.getDepartment() + "，日期" + appointment.getDate() + "，时间" + appointment.getTime()
                    + (isBlank(appointment.getDoctorName()) ? "" : "，医生" + appointment.getDoctorName());
        }
        return "预约失败，请稍后再试";
    }

    @Tool(
            name = "取消预约挂号",
            value = "根据预约信息查询预约挂号是否存在；存在则取消并返回结果，不存在则提示用户核对预约科室和时间。")
    public String cancelAppointment(Appointment appointment){
        if (appointment == null || isIncompleteForCancellation(appointment)) {
            return "取消预约信息不完整，请补充姓名、手机号、身份证号、科室、日期和时间";
        }
        Appointment existAppointment = appointmentService.getOne(appointment);
        if (existAppointment == null) {
            return "您没有预约过该号源，请核对预约科室和时间";
        }
        boolean result = appointmentService.removeById(existAppointment.getId());
        if (result) {
            return "取消预约成功";
        }
        return "取消预约失败，请稍后再试";
    }

    private boolean isIncompleteForBooking(Appointment appointment) {
        return isBlank(appointment.getUsername())
                || isBlank(appointment.getPhone())
                || isBlank(appointment.getIdCard())
                || appointment.getDate() == null
                || appointment.getTime() == null;
    }

    private boolean isIncompleteForCancellation(Appointment appointment) {
        return isBlank(appointment.getUsername())
                || isBlank(appointment.getPhone())
                || isBlank(appointment.getIdCard())
                || isBlank(appointment.getDepartment())
                || appointment.getDate() == null
                || appointment.getTime() == null;
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}

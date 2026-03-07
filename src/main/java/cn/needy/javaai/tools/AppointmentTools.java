package cn.needy.javaai.tools;

import cn.needy.javaai.entity.Appointment;
import cn.needy.javaai.service.AppointmentService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
            value = "根据科室名称，日期，时间和医生查询是否有号源，并返回给用户")
    public boolean queryDepartment(@P(value = "科室名称") String department,
                                  @P(value = "日期") String date,
                                  @P(value = "时间") String time,
                                  @P(value = "医生名称", required = false) String doctorName){
        System.out.println("查询是否有号源,科室名称" + department + "日期" + date + "时间" + time + "医生名称" + doctorName);
        //todo 查询数据库

        return true;
    }

    @Tool(
            name = "预约挂号",
            value="根据参数，先执行queryDepartment方法查询是否有号源，如果有号源，返回说明有号源并且让用户确认后再进行预约。")
    public String bookAppointment(Appointment appointment){
        // 查询是否已经存在预约信息
        Appointment existAppointment = appointmentService.getOne(appointment);
        if (existAppointment != null) {
            return "您已经预约过该号源，请勿重复预约";
        } else {
            // 防止大模型幻觉创建id，这里将id设置为null
            appointment.setId( null);
            // 创建预约信息
            boolean result = appointmentService.save(appointment);
            if (result) {
                return "预约成功，返回预约详情";
            } else {
                return "预约失败，请稍后再试";
            }
        }
    }

    @Tool(
            name = "取消预约",
            value = "根据参数，查询预约是否存在，如果存在则删除预约记录并且返回true,否则返回false")
    public String cancelAppointment(Appointment appointment){
        Appointment existAppointment = appointmentService.getOne(appointment);
        if (existAppointment == null) {
            return "您没有预约过该号源，请核对预约科室和时间";
        }
        // 删除预约信息
        boolean result = appointmentService.removeById(appointment);
        if (result) {
            return "取消预约成功";
        } else {
            return "取消预约失败，请稍后再试";
        }
    }


}

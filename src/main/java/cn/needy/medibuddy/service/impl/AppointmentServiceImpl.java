package cn.needy.medibuddy.service.impl;

import cn.needy.medibuddy.entity.Appointment;
import cn.needy.medibuddy.mapper.AppointmentMapper;
import cn.needy.medibuddy.service.AppointmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-05 10:43
 **/

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {

    /**
     * 查询预约是否存在
     * @param appointment
     * @return 预约信息
     */
    @Override
    public Appointment getOne(Appointment appointment) {
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getUsername, appointment.getUsername())
                .eq(Appointment::getIdCard, appointment.getIdCard())
                .eq(Appointment::getPhone, appointment.getPhone())
                .eq(Appointment::getDate, appointment.getDate())
                .eq(Appointment::getTime, appointment.getTime());

        if (appointment.getDepartment() != null && !appointment.getDepartment().isBlank()) {
            queryWrapper.eq(Appointment::getDepartment, appointment.getDepartment());
        }
        if (appointment.getDoctorName() != null && !appointment.getDoctorName().isBlank()) {
            queryWrapper.eq(Appointment::getDoctorName, appointment.getDoctorName());
        }
        return baseMapper.selectOne(queryWrapper);
    }
}

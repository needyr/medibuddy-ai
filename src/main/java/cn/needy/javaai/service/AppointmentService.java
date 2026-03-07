package cn.needy.javaai.service;

import cn.needy.javaai.entity.Appointment;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-05 10:42
 **/

@Service
public interface AppointmentService extends IService<Appointment> {
    Appointment getOne(Appointment appointment);
}

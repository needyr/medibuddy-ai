package cn.needy.medibuddy;

import cn.needy.medibuddy.entity.Appointment;
import cn.needy.medibuddy.mapper.AppointmentMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class AppointmentMapperTest {

    @Autowired
    private AppointmentMapper appointmentMapper;

    /**
     * 1. 插入一条记录
     */
    @Test
    void testInsert() {
        Appointment a = new Appointment();
        a.setUsername("张三");
        a.setIdCard("123456789012345678");
        a.setDepartment("内科");
        a.setDate("2024-03-06");
        a.setTime("09:30");
        a.setDoctorName("李医生");

        int rows = appointmentMapper.insert(a);
        System.out.println("影响行数: " + rows);
        System.out.println("生成的主键ID: " + a.getId());
    }

    /**
     * 2. 根据主键查询
     */
    @Test
    void testSelectById() {
        Long id = 1L; // 确保数据库中有这条记录
        Appointment a = appointmentMapper.selectById(id);
        System.out.println(a);
    }

    /**
     * 3. 条件查询：查询某个科室的所有预约
     */
    @Test
    void testSelectByDepartment() {
        QueryWrapper<Appointment> wrapper = new QueryWrapper<>();
        wrapper.eq("department", "内科");

        List<Appointment> list = appointmentMapper.selectList(wrapper);
        list.forEach(System.out::println);
    }

    /**
     * 4. 更新：根据主键修改医生姓名
     */
    @Test
    void testUpdateDoctor() {
        Appointment a = new Appointment();
        a.setId(1L); // 要修改的那条记录的ID
        a.setDoctorName("王医生");

        int rows = appointmentMapper.updateById(a);
        System.out.println("更新影响行数: " + rows);
    }

    /**
     * 5. 删除：根据主键删除
     */
    @Test
    void testDeleteById() {
        Long id = 1L; // 要删除的记录ID
        int rows = appointmentMapper.deleteById(id);
        System.out.println("删除影响行数: " + rows);
    }

    /**
     * 6. 查询所有记录
     */
    @Test
    void testSelectAll() {
        List<Appointment> list = appointmentMapper.selectList(null);
        list.forEach(System.out::println);
    }
}
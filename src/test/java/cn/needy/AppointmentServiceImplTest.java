package cn.needy.medibuddy;

import cn.needy.medibuddy.entity.Appointment;
import cn.needy.medibuddy.service.AppointmentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AppointmentServiceImplTest {
    @org.springframework.beans.factory.annotation.Autowired
    private AppointmentService appointmentService;

    /**
     * 场景1：先插入一条记录，再按完全相同的条件用 getOne 查询，应能查到
     */
    @Test
    void testGetOne_exists() {
        // 1. 先保存一条数据
        Appointment insert = new Appointment();
        insert.setUsername("张三");
        insert.setIdCard("123456789012345678");
        insert.setDepartment("内科");
        insert.setDate("2026-03-05");
        insert.setTime("09:00");
        insert.setDoctorName("李医生");

        boolean saveResult = appointmentService.save(insert);
        Assertions.assertTrue(saveResult, "插入预约失败");

        // 2. 构造同样条件的查询对象
        Appointment query = new Appointment();
        query.setUsername("张三");
        query.setIdCard("123456789012345678");
        query.setDepartment("内科");
        query.setDate("2026-03-05");
        query.setTime("09:00");
        query.setDoctorName("李医生");

        // 3. 调用被测试方法
        Appointment result = appointmentService.getOne(query);

        // 4. 断言：能查到，并且字段匹配
        Assertions.assertNotNull(result, "应该能查询到预约记录");
        Assertions.assertEquals(insert.getId(), result.getId());
        Assertions.assertEquals("张三", result.getUsername());
    }

    /**
     * 场景2：使用不存在的条件进行查询，应返回 null
     */
    @Test
    void testGetOne_notExists() {
        Appointment query = new Appointment();
        query.setUsername("不存在的用户");
        query.setIdCard("000000000000000000");
        query.setDepartment("外科");
        query.setDate("2099-01-01");
        query.setTime("00:00");
        query.setDoctorName("不存在的医生");

        Appointment result = appointmentService.getOne(query);

        Assertions.assertNull(result, "不存在的预约条件应该返回 null");
    }

    @Test
    void removeById() {
        boolean removeResult = appointmentService.removeById(1L);
        Assertions.assertTrue(removeResult, "删除预约失败");
    }
}
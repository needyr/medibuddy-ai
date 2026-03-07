package cn.needy;

import cn.needy.javaai.bean.ChatMessages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-04 13:49
 **/

@SpringBootTest
public class MongoCRUDTest {
    @Autowired
    private MongoTemplate mongoTemplate;

    // @Test
    // public void testInsert(){
    //     ChatMessages chatMessages = new ChatMessages();
    //     chatMessages.setMessageId(1L);
    //     chatMessages.setContent("hello world");
    //     mongoTemplate.insert(chatMessages);
    // }

    @Test
    public void testInsert2(){
        ChatMessages chatMessages = new ChatMessages();
        chatMessages.setContent("聊天记录列表");
        mongoTemplate.insert(chatMessages);
    }

    @Test
    public void testFind(){
        ChatMessages chatMessages = mongoTemplate.findById("69a7c9c5004bbb7a142ff138", ChatMessages.class);
        System.out.println(chatMessages);
    }

    @Test
    public void testUpdate(){
        Criteria criteria = Criteria.where("_id").is("69a7c9c5004bbb7a142ff138");
        Query query = Query.query(criteria);
        Update update = new Update();
        update.set("content", "更新后的内容");
        mongoTemplate.upsert(query, update, ChatMessages.class);
    }
}

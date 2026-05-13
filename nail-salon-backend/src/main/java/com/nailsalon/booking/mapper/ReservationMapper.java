package com.nailsalon.booking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nailsalon.booking.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /**
     * 查询预约记录（不受 @TableLogic 影响，可以查到软删除的记录）
     *
     * 📖 知识点：@TableLogic 的作用
     * MyBatis-Plus 的 @TableLogic 会自动给 SELECT 加上 WHERE is_deleted=0
     * 这个自定义方法绕过了这个过滤，用原生 SQL 直接查
     */
    @Select("SELECT * FROM reservation WHERE id = #{id}")
    Reservation selectByIdIncludeDeleted(@Param("id") Long id);

    /**
     * 按联系 ID 统计预约次数（包含软删除的记录）
     */
    @Select("SELECT COUNT(*) FROM reservation WHERE contact_id = #{contactId}")
    int countByContactIdIncludeDeleted(@Param("contactId") String contactId);

    /**
     * 恢复预约（将 is_deleted 设为 0）
     * 返回影响行数：1=成功，0=记录不存在
     */
    @Update("UPDATE reservation SET is_deleted = 0 WHERE id = #{id}")
    int restoreById(@Param("id") Long id);

    /**
     * 查询所有已取消的预约，按日期倒序排列
     */
    @Select("SELECT * FROM reservation WHERE is_deleted = 1 ORDER BY reserve_date DESC, time_slot ASC")
    List<Reservation> selectCancelledList();
}

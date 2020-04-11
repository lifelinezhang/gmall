package com.atguigu.gmall.ums.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 会员收藏的专题活动
 *
 * @author lifeline
 * @email 13001196631@163.com
 * @date 2020-04-09 23:33:13
 */
@ApiModel
@Data
@TableName("ums_member_collect_subject")
public class MemberCollectSubjectEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	@ApiModelProperty(name = "id", value = "id")
	private Long id;
	/**
	 * subject_id
	 */
	@ApiModelProperty(name = "subjectId", value = "subject_id")
	private Long subjectId;
	/**
	 * subject_name
	 */
	@ApiModelProperty(name = "subjectName", value = "subject_name")
	private String subjectName;
	/**
	 * subject_img
	 */
	@ApiModelProperty(name = "subjectImg", value = "subject_img")
	private String subjectImg;
	/**
	 * 活动url
	 */
	@ApiModelProperty(name = "subjectUrll", value = "活动url")
	private String subjectUrll;

}

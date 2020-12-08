package com.guli.edu.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.guli.common.constants.PriceConstants;
import com.guli.common.constants.ResultCodeEnum;
import com.guli.common.exception.GuliException;
import com.guli.edu.dto.CourseInfoDto;
import com.guli.edu.entity.EduCourse;
import com.guli.edu.entity.EduCourseDescription;
import com.guli.edu.entity.EduCourseExample;
import com.guli.edu.mapper.EduCourseDescriptionMapper;
import com.guli.edu.mapper.EduCourseMapper;
import com.guli.edu.query.CourseQuery;
import com.guli.edu.service.EduChapterService;
import com.guli.edu.service.EduCourseService;
import com.guli.edu.service.EduVideoService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EduCourseServiceImpl implements EduCourseService {

    @Autowired
    private EduCourseMapper eduCourseMapper;

    @Autowired
    private EduCourseDescriptionMapper courseDescriptionMapper;

    @Autowired
    private EduVideoService eduVideoService;

    @Autowired
    private EduChapterService eduChapterService;

    @Override
    @Transactional
    public String saveCourseInfo(CourseInfoDto courseInfoDto) {
        EduCourse eduCourse = new EduCourse();
        eduCourse.setStatus(EduCourse.COURSE_DRAFT);
        BeanUtils.copyProperties(courseInfoDto,eduCourse);
        int result = eduCourseMapper.insert(eduCourse);
        if (result <= 0){
            throw new GuliException(ResultCodeEnum.UNKNOWN_REASON);
        }
        EduCourseDescription courseDescription = new EduCourseDescription();
        courseDescription.setId(eduCourse.getId());
        courseDescription.setDescription(courseInfoDto.getDescription());
        int flag = courseDescriptionMapper.insert(courseDescription);
        if (flag <=0 ){
            throw new GuliException(ResultCodeEnum.UNKNOWN_REASON);
        }
        return eduCourse.getId().toString();
    }

    @Override
    public CourseInfoDto getCourseInfoFormById(Long id) {
        EduCourse eduCourse = eduCourseMapper.selectByPrimaryKey(id);
        if (ObjectUtils.isEmpty(eduCourse)) {
            throw new GuliException(ResultCodeEnum.NO_EXISTED_DATA);
        }
        CourseInfoDto courseInfoDto = new CourseInfoDto();
        BeanUtils.copyProperties(eduCourse, courseInfoDto);
        //设置显示精度：舍弃多余的位数
        courseInfoDto.setPrice(courseInfoDto.getPrice()
                .setScale(PriceConstants.DISPLAY_SCALE, BigDecimal.ROUND_FLOOR));
        EduCourseDescription description = courseDescriptionMapper.selectByPrimaryKey(id);
        courseInfoDto.setDescription(description.getDescription());
        return courseInfoDto;
    }

    @Transactional
    @Override
    public void updateCourseInfoById(CourseInfoDto courseInfoDto) {
        //保存课程基本信息
        EduCourse course = new EduCourse();
        course.setStatus(EduCourse.COURSE_DRAFT);
        BeanUtils.copyProperties(courseInfoDto, course);
        int result = eduCourseMapper.updateByPrimaryKeySelective(course);
        if(result <= 0){
            throw new GuliException(ResultCodeEnum.UNKNOWN_REASON);
        }

        //保存课程详情信息
        EduCourseDescription courseDescription = new EduCourseDescription();
        courseDescription.setDescription(courseInfoDto.getDescription());
        courseDescription.setId(course.getId());
        int flag = courseDescriptionMapper.updateByPrimaryKeySelective(courseDescription);
        if(flag <= 0){
            throw new GuliException(ResultCodeEnum.UNKNOWN_REASON);
        }
    }

    @Override
    public Map getPageList(int page, int limit, CourseQuery courseQuery) {

        Page<Object> startPage = PageHelper.startPage(page, limit);
        // 结果返回Map
        Map<String, Object> resultMap = new HashMap<>();
        List<EduCourse> eduCourses = new ArrayList<>();
        EduCourseExample courseExample = new EduCourseExample();
        courseExample.setOrderByClause("gmt_create DESC");
        if (ObjectUtils.isEmpty(courseQuery)) {
            eduCourses = eduCourseMapper.selectByExample(courseExample);
        } else {
            EduCourseExample.Criteria criteria = courseExample.createCriteria();
            String title = courseQuery.getTitle();
            Long teacherId = courseQuery.getTeacherId();
            Long subjectParentId = courseQuery.getSubjectParentId();
            Long subjectId = courseQuery.getSubjectId();
            if (!StringUtils.isEmpty(title)) {
                criteria.andTitleLike(title);
            }

            if (!StringUtils.isEmpty(teacherId) ) {
                criteria.andTeacherIdEqualTo(teacherId);
            }

            if (!StringUtils.isEmpty(subjectParentId)) {
                criteria.andSubjectParentIdEqualTo(subjectParentId);
            }

            if (!StringUtils.isEmpty(subjectId)) {
                criteria.andSubjectIdEqualTo(subjectId);
            }
            eduCourses = eduCourseMapper.selectByExample(courseExample);
        }
        resultMap.put("total",startPage.getTotal());
        resultMap.put("rows",eduCourses);
        return resultMap;
    }

    @Transactional
    @Override
    public boolean removeCourseById(Long courseId) {
        // 如果用户确定删除，则首先删除video记录，然后删除chapter记录，最后删除Course记录
        eduVideoService.deleteVideoByCourseId(courseId);
        eduChapterService.removeByCourseId(courseId);
        int result = eduCourseMapper.deleteByPrimaryKey(courseId);
        return result > 0 ;
    }
}

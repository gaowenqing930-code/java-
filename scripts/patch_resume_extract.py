# -*- coding: utf-8 -*-
"""给 InterviewController 加 /resume/extract 接口"""
import pathlib

p = pathlib.Path(r"D:\Codes\ai-interview-coach\backend\src\main\java\com\xbmd\module\interview\controller\InterviewController.java")
s = p.read_text(encoding="utf-8")

old_imports = """import com.xbmd.module.interview.dto.ResumeParseVO;
import com.xbmd.module.interview.service.InterviewService;
import com.xbmd.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;"""

new_imports = """import com.xbmd.module.interview.dto.ResumeExtractVO;
import com.xbmd.module.interview.dto.ResumeParseVO;
import com.xbmd.module.interview.service.InterviewService;
import com.xbmd.module.interview.support.ResumeTextExtractor;
import com.xbmd.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;"""

assert old_imports in s, "imports block not found"
s = s.replace(old_imports, new_imports)

old_field = "    private final InterviewService interviewService;"
new_field = """    private final InterviewService interviewService;
    private final ResumeTextExtractor resumeTextExtractor;"""
assert old_field in s
s = s.replace(old_field, new_field)

old_create = """    /** 解析简历并创建面试会话 */
    @PostMapping("/sessions")"""
new_create = """    /** 上传简历文件（.docx/.doc/.pdf），提取文本供用户确认 */
    @PostMapping("/resume/extract")
    public Result<ResumeExtractVO> extract(@RequestParam("file") MultipartFile file) {
        ResumeExtractVO vo = new ResumeExtractVO();
        vo.setFileName(file.getOriginalFilename());
        vo.setText(resumeTextExtractor.extract(file));
        return Result.success(vo);
    }

    /** 解析简历并创建面试会话 */
    @PostMapping("/sessions")"""
assert old_create in s
s = s.replace(old_create, new_create)

p.write_text(s, encoding="utf-8", newline="")
print("OK")

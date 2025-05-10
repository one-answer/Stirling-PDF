package stirling.software.SPDF.controller.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Hidden;

import stirling.software.SPDF.model.ApplicationProperties;
import stirling.software.SPDF.model.Dependency;

@Controller
public class HomeWebController {

    @GetMapping("/about")
    @Hidden
    public String gameForm(Model model) {
        model.addAttribute("currentPage", "about");
        return "about";
    }

    @GetMapping("/licenses")
    @Hidden
    public String licensesForm(Model model) {
        model.addAttribute("currentPage", "licenses");
        Resource resource = new ClassPathResource("static/3rdPartyLicenses.json");
        try {
            InputStream is = resource.getInputStream();
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, List<Dependency>> data =
                    mapper.readValue(json, new TypeReference<Map<String, List<Dependency>>>() {});
            model.addAttribute("dependencies", data.get("dependencies"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "licenses";
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("currentPage", "home");
        return "home";
    }

    @GetMapping("/home")
    public String root(Model model) {
        return "redirect:/";
    }

    @Autowired ApplicationProperties applicationProperties;

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    @Hidden
    public String getRobotsTxt() {
        Boolean allowGoogle = applicationProperties.getSystem().getGooglevisibility();
        if (Boolean.TRUE.equals(allowGoogle)) {
            return "User-agent: Googlebot\nAllow: /\n\nUser-agent: *\nAllow: /\n\nSitemap: "
                    + getSiteUrl()
                    + "/sitemap.xml";
        } else {
            return "User-agent: Googlebot\nDisallow: /\n\nUser-agent: *\nDisallow: /";
        }
    }

    private String getSiteUrl() {
        String siteUrl = applicationProperties.getUi().getSiteUrl();
        return (siteUrl != null && !siteUrl.isEmpty()) ? siteUrl : "";
    }

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    @Hidden
    public String generateSitemap() {
        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        String baseUrl = getSiteUrl();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        // Add home page
        sitemap.append("  <url>\n");
        sitemap.append("    <loc>").append(baseUrl).append("/</loc>\n");
        sitemap.append("    <lastmod>").append(today).append("</lastmod>\n");
        sitemap.append("    <changefreq>weekly</changefreq>\n");
        sitemap.append("    <priority>1.0</priority>\n");
        sitemap.append("  </url>\n");

        // Get all GET endpoints that return HTML pages
        List<String> endpoints = new ArrayList<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            // Skip endpoints with @Hidden annotation
            if (handlerMethod.getMethodAnnotation(Hidden.class) != null) {
                continue;
            }

            // Only include GET endpoints that return HTML (not API endpoints)
            if (mappingInfo.getMethodsCondition().getMethods().isEmpty()
                    || mappingInfo
                            .getMethodsCondition()
                            .getMethods()
                            .contains(org.springframework.web.bind.annotation.RequestMethod.GET)) {

                if (mappingInfo.getPatternsCondition() != null
                        && !mappingInfo.getPatternsCondition().getPatterns().isEmpty()) {
                    for (String pattern : mappingInfo.getPatternsCondition().getPatterns()) {
                        // Skip API endpoints and other non-HTML endpoints
                        if (!pattern.startsWith("/api/")
                                && !pattern.endsWith(".json")
                                && !pattern.endsWith(".xml")
                                && !pattern.endsWith(".txt")
                                && !pattern.equals("/error")) {

                            endpoints.add(pattern);
                        }
                    }
                }
            }
        }

        // Add all valid endpoints to sitemap
        for (String endpoint : endpoints) {
            sitemap.append("  <url>\n");
            sitemap.append("    <loc>").append(baseUrl).append(endpoint).append("</loc>\n");
            sitemap.append("    <lastmod>").append(today).append("</lastmod>\n");
            sitemap.append("    <changefreq>monthly</changefreq>\n");
            sitemap.append("    <priority>0.8</priority>\n");
            sitemap.append("  </url>\n");
        }

        sitemap.append("</urlset>");
        return sitemap.toString();
    }
}

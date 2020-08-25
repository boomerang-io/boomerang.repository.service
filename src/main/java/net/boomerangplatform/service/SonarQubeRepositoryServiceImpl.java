package net.boomerangplatform.service;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import net.boomerangplatform.model.Analysis;
import net.boomerangplatform.model.CICDSettingsEntity;
import net.boomerangplatform.model.Config;
import net.boomerangplatform.model.Event;
import net.boomerangplatform.model.History;
import net.boomerangplatform.model.IssueComponent;
import net.boomerangplatform.model.Issues;
import net.boomerangplatform.model.Measure;
import net.boomerangplatform.model.Measures;
import net.boomerangplatform.model.SonarQubeDetailReport;
import net.boomerangplatform.model.SonarQubeIssue;
import net.boomerangplatform.model.SonarQubeIssuesReport;
import net.boomerangplatform.model.SonarQubeMeasuresReport;
import net.boomerangplatform.model.SonarQubeProjectVersions;
import net.boomerangplatform.model.SonarQubeReport;
import net.boomerangplatform.util.DateUtil;

@Service
public class SonarQubeRepositoryServiceImpl implements SonarQubeRepositoryService {

  @Value("${ci.rest.url.base}")
  private String ciUrlBase;

  @Value("${ci.rest.url.get.internal.setting}")
  private String internalSettingUrl;

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;

  private static final String SONAR_URL_BASE = "sonarqube.url.api.base";

  private static final String COMPONENT = "{component}";

  // private static final Logger LOGGER = LogManager.getLogger();
  //
  // private static final String LOG_INFO = "ciComponentName=%s, ciComponentVersionId=%s,
  // ciTeamId=%s";

  @Value("${sonarqube.url.api.metrics.violations}")
  private String sonarqubeUrlApiMetricsViolations;

  @Value("${sonarqube.url.api.metrics.testcoverage}")
  private String sonarqubeUrlApiMetricsTestCoverage;

  @Value("${sonarqube.url.api.project.versions}")
  private String sonarqubeUrlApiProjectVersions;

  @Value("${sonarqube.url.api.issues.version}")
  private String sonarqubeUrlApiIssuesVersion;

  @Value("${sonarqube.url.api.measures.version}")
  private String sonarqubeUrlApiMeasuresVersion;

  @Value("${sonarqube.url.api.issues.latest}")
  private String sonarqubeUrlApiIssuesLatest;

  @Value("${sonarqube.url.api.measures.latest}")
  private String sonarqubeUrlApiMeasuresLatest;

  @Value("${sonarqube.url.api.measures.componenttree}")
  private String sonarqubeUrlApiMeasuresComponentTree;

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate internalRestTemplate;

  @Override
  public SonarQubeReport getReport(String id, String version) {
    String sonarqubeUrlApiBase = getSonarqubeUrlBase();
    SonarQubeReport sonarQubeReport = new SonarQubeReport();

    Date date = getSonarQubeDateForVersion(id, version);
    if (date != null) {
      StringBuilder sb = new StringBuilder();
      sb.append(sonarqubeUrlApiBase).append(sonarqubeUrlApiMeasuresVersion)
          .append(sonarqubeUrlApiMetricsViolations);

      String url = getUrl(sb.toString(), id, date);

      final HttpEntity<?> request = new HttpEntity<>(getHeaders());

      final ResponseEntity<SonarQubeMeasuresReport> sonarQubeMeasuresReportResponse =
          internalRestTemplate.exchange(url, HttpMethod.GET, request,
              SonarQubeMeasuresReport.class);
      SonarQubeMeasuresReport sonarQubeMeasuresReport = sonarQubeMeasuresReportResponse.getBody();

      Measures measures = getMeasures(sonarQubeMeasuresReport.getMeasures());

      final ResponseEntity<SonarQubeIssuesReport> sonarQubeReportResponse =
          internalRestTemplate.exchange(getSonarQubeReportUrl(id, date), HttpMethod.GET, request,
              SonarQubeIssuesReport.class);
      SonarQubeIssuesReport sonarQubeIssuesReport = sonarQubeReportResponse.getBody();

      Issues issues =
          getIssues(sonarQubeIssuesReport.getIssues(), sonarQubeIssuesReport.getComponents());

      sonarQubeReport.setIssues(issues);
      sonarQubeReport.setMeasures(measures);
    }
    return sonarQubeReport;
  }

  @Override
  public SonarQubeReport getTestCoverageReport(String id, String version) {

    SonarQubeReport sonarQubeReport = new SonarQubeReport();

    Date date = getSonarQubeDateForVersion(id, version);
    if (date != null) {

      StringBuilder sb = new StringBuilder();
      String sonarqubeUrlApiBase = getSonarqubeUrlBase();
      sb.append(sonarqubeUrlApiBase).append(sonarqubeUrlApiMeasuresVersion)
          .append(sonarqubeUrlApiMetricsTestCoverage);

      String url = getUrl(sb.toString(), id, date);

      final HttpEntity<?> request = new HttpEntity<>(getHeaders());

      final ResponseEntity<SonarQubeMeasuresReport> sonarQubeMeasuresReportResponse =
          internalRestTemplate.exchange(url, HttpMethod.GET, request,
              SonarQubeMeasuresReport.class);
      SonarQubeMeasuresReport sonarQubeMeasuresReport = sonarQubeMeasuresReportResponse.getBody();

      Measures measures = getMeasures(sonarQubeMeasuresReport.getMeasures());

      sonarQubeReport = new SonarQubeReport();
      sonarQubeReport.setIssues(null);
      sonarQubeReport.setMeasures(measures);
    }

    return sonarQubeReport;
  }

  @Override
  public SonarQubeDetailReport getDetailTestCoverageReport(String id, String version) {

    StringBuilder sb = new StringBuilder();
    String sonarqubeUrlApiBase = getSonarqubeUrlBase();
    sb.append(sonarqubeUrlApiBase).append(sonarqubeUrlApiMeasuresComponentTree)
        .append(sonarqubeUrlApiMetricsTestCoverage);

    String url = sb.toString().replace(COMPONENT, id);

    final HttpEntity<?> request = new HttpEntity<>(getHeaders());

    final ResponseEntity<SonarQubeDetailReport> sonarQubeDetailReportResponse =
        internalRestTemplate.exchange(url, HttpMethod.GET, request, SonarQubeDetailReport.class);

    return sonarQubeDetailReportResponse.getBody();
  }

  private HttpHeaders getHeaders() {

    String sonarqubeBoomerangApitoken = getSonarqubeApiToken();

    final String plainCreds = sonarqubeBoomerangApitoken + ":";
    final byte[] plainCredsBytes = plainCreds.getBytes(StandardCharsets.UTF_8);
    final byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
    final String base64Creds = new String(base64CredsBytes, StandardCharsets.UTF_8);

    final HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "application/json");
    headers.add("Authorization", "Basic " + base64Creds);

    return headers;
  }

  protected String getSonarqubeApiToken() {
    String settingUrl = ciUrlBase + internalSettingUrl + "repository";

    final HttpEntity<String> settingRequest = new HttpEntity<>(buildHeaders());

    ResponseEntity<CICDSettingsEntity> responseEntity = restTemplate.exchange(settingUrl,
        HttpMethod.GET, settingRequest, new ParameterizedTypeReference<CICDSettingsEntity>() {});
    CICDSettingsEntity settings = responseEntity.getBody();

    String sonarqubeBoomerangApitoken = null;
    if (settings != null) {
      for (Config config : settings.getConfig()) {
        if (config.getKey().equals("sonarqube.boomerang.apitoken")) {
          sonarqubeBoomerangApitoken = config.getValue();
        }
      }
    }
    return sonarqubeBoomerangApitoken;
  }

  private String getSonarQubeReportUrl(String id, Date date) {
    StringBuilder sb = new StringBuilder();
    String sonarqubeUrlApiBase = getSonarqubeUrlBase();

    sb.append(sonarqubeUrlApiBase).append(sonarqubeUrlApiIssuesVersion);

    return sb.toString().replace("{componentKeys}", id).replace("{createdBefore}",
        dateToString(addSecond(date)));
  }

  private String getUrl(String url, String componentId, Date date) {
    return url.replace(COMPONENT, componentId).replace("{from}", dateToString(date)).replace("{to}",
        dateToString(date));
  }

  private Date getSonarQubeDateForVersion(String project, String version) {

    final HttpEntity<?> request = new HttpEntity<>(getHeaders());

    StringBuilder sb = new StringBuilder();
    String sonarqubeUrlApiBase = getSonarqubeUrlBase();

    sb.append(sonarqubeUrlApiBase).append(sonarqubeUrlApiProjectVersions);

    String url = sb.toString().replace("{project}", project);

    final ResponseEntity<SonarQubeProjectVersions> sonarQubeProjectVersionsResponse =
        internalRestTemplate.exchange(url, HttpMethod.GET, request, SonarQubeProjectVersions.class);
    SonarQubeProjectVersions sonarQubeProjectVersions = sonarQubeProjectVersionsResponse.getBody();

    for (Analysis analysis : sonarQubeProjectVersions.getAnalyses()) {
      for (Event event : analysis.getEvents()) {
        if (event.getName().equalsIgnoreCase(version)) {
          return analysis.getDate();
        }
      }
    }

    return null;
  }

  protected String getSonarqubeUrlBase() {
    String settingUrl = ciUrlBase + internalSettingUrl + "repository";

    final HttpEntity<String> settingRequest = new HttpEntity<>(buildHeaders());

    ResponseEntity<CICDSettingsEntity> responseEntity = restTemplate.exchange(settingUrl,
        HttpMethod.GET, settingRequest, new ParameterizedTypeReference<CICDSettingsEntity>() {});
    CICDSettingsEntity settings = responseEntity.getBody();

    String sonarqubeUrlApiBase = null;
    if (settings != null) {
      for (Config config : settings.getConfig()) {
        if (config.getKey().equals(SONAR_URL_BASE)) {
          sonarqubeUrlApiBase = config.getValue();
        }
      }
    }
    return sonarqubeUrlApiBase;
  }

  private Measures getMeasures(List<Measure> measureList) {
    Measures measures = new Measures();
    for (Measure measure : measureList) {
      for (History history : measure.getHistory()) {
        setInfo(measures, measure, history);
      }
    }
    return measures;
  }

  private void setInfo(Measures measures, Measure measure, History history) {// NOSONAR
    switch (measure.getMetric()) {
      case "ncloc":
        measures.setNcloc(sanityIntegerValue(history.getValue()));
        break;
      case "complexity":
        measures.setComplexity(sanityIntegerValue(history.getValue()));
        break;
      case "violations":
        measures.setViolations(sanityIntegerValue(history.getValue()));
        break;
      case "tests":
        measures.setTests(sanityIntegerValue(history.getValue()));
        break;
      case "test_errors":
        measures.setTestErrors(sanityIntegerValue(history.getValue()));
        break;
      case "test_failures":
        measures.setTestFailures(sanityIntegerValue(history.getValue()));
        break;
      case "skipped_tests":
        measures.setSkippedTests(sanityIntegerValue(history.getValue()));
        break;
      case "test_success_density":
        measures.setTestSuccessDensity(sanityDoubleValue(history.getValue()));
        break;
      case "test_execution_time":
        measures.setTestExecutionTime(sanityIntegerValue(history.getValue()));
        break;
      case "coverage":
        measures.setCoverage(sanityDoubleValue(history.getValue()));
        break;
      case "lines_to_cover":
        measures.setLinesToCover(sanityIntegerValue(history.getValue()));
        break;
      case "uncovered_lines":
        measures.setUncoveredLines(sanityIntegerValue(history.getValue()));
        break;
      case "line_coverage":
        measures.setLineCoverage(sanityDoubleValue(history.getValue()));
        break;
      default:
        break;
    }
  }

  private Integer sanityIntegerValue(String value) {
    return value == null ? 0 : Integer.valueOf(value);
  }

  private Double sanityDoubleValue(String value) {
    return value == null ? 0 : Double.valueOf(value);
  }

  private Issues getIssues(List<SonarQubeIssue> sonarQubeIssues,
      List<IssueComponent> issueComponents) {
    int total = 0;
    int blocker = 0;
    int critical = 0;
    int major = 0;
    int minor = 0;
    int info = 0;
    int filesAnalyzed = 0;

    for (SonarQubeIssue sonarQubeIssue : sonarQubeIssues) {
      if ("open".equalsIgnoreCase(sonarQubeIssue.getStatus())) {
        switch (sonarQubeIssue.getSeverity()) {
          case "BLOCKER":
            blocker++;
            break;
          case "CRITICAL":
            critical++;
            break;
          case "MAJOR":
            major++;
            break;
          case "MINOR":
            minor++;
            break;
          case "INFO":
            info++;
            break;
          default:
            break;
        }
        total++;
      }
    }

    for (IssueComponent issueComponent : issueComponents) {
      if ("FIL".equals(issueComponent.getQualifier())) {
        filesAnalyzed++;
      }
    }

    Issues issues = new Issues();
    issues.setTotal(total);
    issues.setBlocker(blocker);
    issues.setCritical(critical);
    issues.setMajor(major);
    issues.setMinor(minor);
    issues.setInfo(info);
    issues.setFilesAnalyzed(filesAnalyzed);

    return issues;
  }

  private String dateToString(Date date) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    return dateFormat.format(date);
  }

  private Date addSecond(Date date) {
    LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    return DateUtil.asDate(localDateTime.plusSeconds(1L));
  }

  private HttpHeaders buildHeaders() {
    final HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "application/json");
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}

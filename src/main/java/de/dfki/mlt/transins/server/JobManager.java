package de.dfki.mlt.transins.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dfki.mlt.transins.server.Job.Status;

/**
 * Manage translation jobs.
 *
 * @author Jörg Steffen, DFKI
 */
public class JobManager {

  private static final Logger logger = LoggerFactory.getLogger(JobManager.class);

  private static final int MAX_JOB_LIST_SIZE = 100;
  private List<String> finishedJobs;
  private List<String> failedJobs;

  private Map<String, Job> jobs;

  private String inputFolder;
  private String outputFolder;


  /**
   * Create a new job manager. Files associated with the jobs are stored in the given input and
   * output folders.
   *
   * @param inputFolder
   *          the input folder
   * @param outputFolder
   *          the output folder
   */
  public JobManager(String inputFolder, String outputFolder) {

    this.inputFolder = inputFolder;
    this.outputFolder = outputFolder;

    this.finishedJobs = new ArrayList<String>(MAX_JOB_LIST_SIZE + 1);
    this.failedJobs = new ArrayList<String>(MAX_JOB_LIST_SIZE + 1);

    this.jobs = new HashMap<>();
  }


  /**
   * Add given job to queued jobs.
   *
   * @param job
   *          the job
   */
  public synchronized void addJobToQueue(Job job) {

    logger.info("add job to queue: {}", job.getId());
    job.setStatus(Status.QUEUED);
    this.jobs.put(job.getId(), job);
  }


  /**
   * Mark given job as in translation.
   *
   * @param jobId
   *          the job id
   */
  public synchronized void markJobAsInTranslation(String jobId) {

    Job job = this.jobs.get(jobId);
    if (job != null && job.getStatus() == Status.QUEUED) {
      logger.info("mark job as in translation: {}", jobId);
      job.setStatus(Status.IN_TRANSLATION);
    } else {
      logger.error("unknown queued job: {}", jobId);
    }
  }


  /**
   * Mark given job as finished.
   *
   * @param jobId
   *          the job id
   */
  public synchronized void markJobAsFinished(String jobId) {

    Job job = this.jobs.get(jobId);
    if (job != null && job.getStatus() == Status.IN_TRANSLATION) {
      logger.info("mark job as finished: {}", jobId);
      job.setStatus(Status.FINISHED);
      this.finishedJobs.add(0, jobId);
      cleanUpJobList(this.finishedJobs);
    } else {
      logger.error("unknown in translation job: {}", jobId);
    }
  }


  /**
   * Mark given job as failed.
   *
   * @param jobId
   *          the job id
   */
  public synchronized void markJobAsFailed(String jobId) {

    Job job = this.jobs.get(jobId);
    if (job != null && job.getStatus() == Status.IN_TRANSLATION) {
      logger.info("mark job as failed: {}", jobId);
      job.setStatus(Status.FAILED);
      this.failedJobs.add(0, jobId);
      cleanUpJobList(this.failedJobs);
    } else {
      logger.error("unknown in translation job: {}", jobId);
    }
  }


  /**
   * Get status for given job.
   *
   * @param jobId
   *          the job id
   * @return the job's status
   */
  public synchronized Status getStatus(String jobId) {

    Job job = this.jobs.get(jobId);
    if (job != null) {
      return job.getStatus();
    }
    return Status.UNKONWN;
  }


  /**
   * Get internal file name for given job.
   *
   * @param jobId
   *          the job id
   * @return the internal file name
   */
  public synchronized String getInternalFileName(String jobId) {

    return this.jobs.get(jobId).getInternalFileName();
  }


  /**
   * Get file name of translated document for given job.
   *
   * @param jobId
   *          the job id
   * @return the file name of translated document as delivered to caller
   */
  public synchronized String getResultFileName(String jobId) {

    return this.jobs.get(jobId).getResultFileName();
  }


  /**
   * Check if the given job list contains more than the maximum number of entries. If yes, delete
   * files associated with the oldest jobs.
   *
   * @param jobList
   *          the job list to check
   */
  private void cleanUpJobList(List<String> jobList) {

    try {
      while (jobList.size() > MAX_JOB_LIST_SIZE) {
        Job jobToDelete = this.jobs.get(jobList.remove(jobList.size() - 1));
        // delete file associated with oldest job in job list in input folder
        try {
          Files.delete(Paths.get(this.inputFolder).resolve(jobToDelete.getInternalFileName()));
          logger.info("deleted old job input file: {} ", jobToDelete.getInternalFileName());
        } catch (NoSuchFileException e) {
          // nothing to do
        }
        // delete file associated with oldest job in job list in output folder
        try {
          Files.delete(Paths.get(this.outputFolder).resolve(jobToDelete.getInternalFileName()));
          logger.info("deleted old job output file: {} ", jobToDelete.getInternalFileName());
        } catch (NoSuchFileException e) {
          // nothing to do
        }
      }
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
  }
}

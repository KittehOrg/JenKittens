package org.kitteh.jenkins.jenkittens;

import com.google.gson.Gson;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Adorable Kitteh builder
 */
public class KittehBuilder extends Builder {
    private final boolean meow;
    private final String secret;
    private final String url;

    @DataBoundConstructor
    @SuppressWarnings("unused")
    public KittehBuilder(boolean meow, String secret, String url) {
        this.meow = meow;
        this.url = url;
        this.secret = secret;
    }

    @SuppressWarnings("unused")
    private static class JenkinsData {
        private String url;
        private String projectName;
        private String result;
        private long duration;
        private String secret;
    }

    @SuppressWarnings("unused")
    private static class JenkinsDataMeow extends JenkinsData {
        private final boolean meow = true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        Gson gson = new Gson();
        if (build.getResult() == null) {
            return true; // Whaaaaaaaat, it's null?
        }
        Result currentResult = build.getResult();
        String result = currentResult.toString();
        Run<?, ?> last = build.getPreviousBuild();
        if (last != null) {
            Result previousResult = last.getResult();
            if (currentResult == Result.SUCCESS && previousResult != Result.SUCCESS) {
                result += " - ALL BETTER"; // Success after failure!
            } else if (currentResult == Result.FAILURE && previousResult == Result.FAILURE) {
                result += " - TODAY IS NOT A GOOD DAY"; // Two or more failures in a row!
            }
        }
        JenkinsData data = this.meow ? new JenkinsDataMeow() : new JenkinsData();
        data.duration = build.getDuration();
        data.url = Jenkins.getInstance().getRootUrl() + build.getUrl();
        data.projectName = build.getProject().getName();
        data.result = result;
        data.secret = this.secret;
        String json = gson.toJson(data, JenkinsData.class);

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(this.url).openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);
            connection.connect();
            DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            output.writeBytes(URLEncoder.encode(json, "UTF-8"));
            output.flush();
            output.close();
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            /* Hello debug
            String line = null;
            listener.getLogger().println("Pushing data to " + this.url);
            listener.getLogger().println("--------------------------");
            while ((line = input.readLine()) != null) {
                listener.getLogger().println(line);
            }
            listener.getLogger().println("--------------------------");
            */
            input.close();
        } catch (IOException ignored) {
        }

        return true;
    }

    @Extension
    // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true; // Yes, use me any time!
        }

        @Override
        public String getDisplayName() {
            return "JenKittens";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            return super.configure(req, formData);
        }
    }
}

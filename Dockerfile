FROM fluent/fluentd:v1.16-debian
USER root
RUN gem install fluent-plugin-azure-loganalytics
USER fluent
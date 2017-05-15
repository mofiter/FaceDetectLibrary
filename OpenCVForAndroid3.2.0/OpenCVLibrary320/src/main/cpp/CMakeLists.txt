cmake_minimum_required(VERSION 3.4.1)

add_library(DetectionBasedTracker_jni SHARED
            DetectionBasedTracker_jni.cpp)

# Include libraries needed for libserial_port lib
target_link_libraries(DetectionBasedTracker_jni
                      android
                      log)
#include <sstream>
#include <string>

namespace example {

std::string hello(int world_count) {
    std::stringstream ss;
    ss << "Hello, there are " << world_count << "worlds";
    return ss.str();
}

}

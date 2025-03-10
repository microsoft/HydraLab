import axios from 'axios'

let instance = axios.create({
    baseURL: '/'
})

const NO_LOGIN_CODE = 401

instance.interceptors.request.use(
    config => {
        console.log("origin: " + window.location);
        // config.headers['Origin'] = window.location.origin;
        config.headers['Origin'] = "window.location.origin";
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

instance.interceptors.response.use(response => {
    if (response.status === NO_LOGIN_CODE) {
        location.reload()
    } else {
        return response
    }
}, error => {
    console.log(error)
    if(error.response.status === NO_LOGIN_CODE){
        location.reload()
    }else{
        return error
    }
 
})

export default instance

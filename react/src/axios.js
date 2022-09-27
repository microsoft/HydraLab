import axios from 'axios'

let instance = axios.create({
    baseURL: '/'
})

const NO_LOGIN_CODE = 401

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

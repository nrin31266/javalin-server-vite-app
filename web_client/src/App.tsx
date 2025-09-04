
import Management from './pages/Management'
import { Provider } from 'react-redux'
import store from './redux/store'
import WebSocketProvider from './ws/WebSocketProvider'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import TestPage from './pages/TestPage'

const App = () => {
  return (
    <Provider store={store}>
      <WebSocketProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Management />} />
            <Route path="/test" element={<TestPage />} />
          </Routes>
        </BrowserRouter>
      </WebSocketProvider>
    </Provider>
  )
}

export default App